// PskRxAlgorithms.h: interface for the CPskRxAlgorithms class.
//
//////////////////////////////////////////////////////////////////////

#ifndef PSKRXALGORITHMS_H
#define PSKRXALGORITHMS_H

#include "psk.h"

#ifdef _DEBUG
	#include <stdio.h>
	extern	FILE *DbgOutFile;
#endif

#include "PskBitDe.h"

#include "SineTab.h"


/*class CPskRxAlgorithms
**This class is an implementation of a PSK decoder using the algorithms designed by
**G3PLX. The drill is that you give this class each sample as it comes in, and it
**decodes the bytes and calls the decoder at 31.25 baud. You provide a decoder
**as a pointer to a CPskBitDecode (an abstract base class) at the SetDecoder() call.
**
**The decoder is called with the value of the I/Q channels, so the decoder gets
**to decide whether to interpret as BPSK or as QPSK.
*/

class CPskRxAlgorithms  
{
public:
	CPskRxAlgorithms();
	~CPskRxAlgorithms();

	//Set the filter state at the beginning of a receive session
	void ResetFilters();

	//The audio frequency of the PSK coming from the radio receiver
	void SetCenterFrequency(long);
	long	GetCenterFrequency(){return m_CenterFrequency;};

	//tell this class the rate the audio is being digitized
	void SetSampleRate(long);

	//you have to call SetDecoder to plug in a BPSK or QPSK decoder
	void SetDecoder(CPskBitDecode *p){m_Decode = p;}

	//These two functions are a hook in case someone wants to run an FFT
	FilterElement_t	GetIRxFil(unsigned i){return m_IRxFil[i];};
	FilterElement_t GetQRxFil(unsigned i){return m_QRxFil[i];};

	void SetSineTable(CSineTable *p){m_SineTable = p;}

	void GetIrxQrx(int i, FilterElement_t &Irx, FilterElement_t &Qrx)
	{
		i += m_RxFilPtr;
		i &= 63;
		Irx = m_IRxFil[i];
		Qrx = m_QRxFil[i];
	}

	//call here with individual samples. Note it is compiled inline for speed.
	//This routine is called m_SampleRate times per second
	void inline AddRxSample(FilterElement_t t)
	{
		long NextPhase;	//long enough to contain the 17th bit when 16 bit adds overflow
		FilterElement_t RxCos, RxSin;

		NextPhase = (long)m_RxPhase + (long)m_RxFreq;
		m_RxPhase = (unsigned short)NextPhase;

		RxCos = t * m_SineTable->CosTblLookup(m_RxPhase); 
		RxCos >>= 16;			//Keep only 16 bit result
		RxSin = t * m_SineTable->SinTblLookup(m_RxPhase); 
		RxSin >>= 16;			//only 16 bits as above

		//moving averages take the fast sample rate down to 500 Hz
		ApplyFastFilter(RxCos, m_ISum1, m_ISum2, m_IDecFil1, m_IDecFil2);
		ApplyFastFilter(RxSin, m_QSum1, m_QSum2, m_QDecFil1, m_QDecFil2);


		if (++m_DecPtr >= m_DecSize)
			m_DecPtr = 0;

		NextPhase = (long)m_DecPhase + (long)m_DecFreq;
		m_DecPhase = (unsigned short)NextPhase;
		if (NextPhase > 0xFFFF)
		{	
			//proceed with 500 Hz processing

			//The shift right keeps the narrow filter within 32 bits
			//the 10 bits come from the scale up that's possible in ApplyFastFilter
			m_QRxFil[m_RxFilPtr] = m_QSum2 >> 10;
			m_IRxFil[m_RxFilPtr] = m_ISum2 >> 10;

			
			m_RxFilPtr += 1;
			m_RxFilPtr &= 63;
			
			FilterElement_t ISum, QSum;

			ApplyNarrowFilter(ISum, QSum);

			int	level = 30;
			long ampl = (ISum >> 16) * (ISum >> 16) + (QSum >> 16) * (QSum >> 16);

			while (ampl > 0)
			{
				level += 1;
				ampl >>= 1;
			}

			//Select 1 of 16
			int BitPhaseInt = m_RxBitPhase >> 12;
			m_RxAmpFil[BitPhaseInt] = (FilterElement_t)level;

			int i;
			ampl = 0;
			for (i = 0; i < BITFILTERLENGTH/2; i += 1)
			{
				ampl += m_RxAmpFil[i] - m_RxAmpFil[i + (BITFILTERLENGTH/2)];
			}

#ifdef _DEBUG
			m_PreviousAmpl = (FilterElement_t)ampl;
#endif

			//The correction is the amplitude times a synchronization gain, which is empirical.
			long BitPhaseCorrection = (long) (ampl * 4);

			NextPhase = (long)m_RxBitPhase + (long) m_RxBitFreq - BitPhaseCorrection;
			m_RxBitPhase = (unsigned short)NextPhase;

			if (NextPhase > 0xFFFF)
			{
#ifdef _DEBUG
				if (m_Decode)
					m_Decode->RetrievePrevious(m_PreviousISum, m_PreviousQSum);
#endif
				//We're at the centre of the bit:  31.25 Hz
				if (m_Decode)
					m_Decode->Decode(ISum, QSum, level);
			}

		}

#ifdef _DEBUG
		if (DbgOutFile)
		{
			static int x;

			x ++;
			//put some filter parameters in an output file so they can be plotted...
			FilterElement_t I, Q;
			if (m_Decode && x < 30000)
			{
				m_Decode->RetrievePrevious(I, Q);
				FilterElement_t Dot = (m_PreviousISum  * I ) + 
										(m_PreviousQSum  * Q ) ;
				fprintf(DbgOutFile, "%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", 
						t, 
						(m_RxBitPhase * 5) / 4 - 32000,
						m_PreviousAmpl * 2000, 
						m_ISum2 / 0x20,
						m_QSum2 / 0x20,
						m_PreviousISum ,
						m_PreviousQSum ,
						Dot / 0x8000);
			}
		}
#endif


	};

protected:
	enum	{
				DECMAX = 32,
				FILTERLENGTH = 64,
				BITFILTERLENGTH = 16};
	
	void AdjustForNewParams();
	CSineTable	*m_SineTable;

	CPskBitDecode *m_Decode;

	unsigned short	m_RxPhase;
	unsigned short	m_RxFreq;			//fraction of m_SampleRate
	unsigned short  m_DecPhase;
	unsigned short	m_DecFreq;			//fraction of m_SampleRate
	unsigned short  m_RxBitPhase;
	unsigned short	m_RxBitFreq;		//fraction of m_DecFreq;

	long	m_CenterFrequency;			//Hz
	long	m_SampleRate;				//samples/second

	int		m_DecPtr;
	int		m_DecSize;
	int		m_RxFilPtr;

	FilterElement_t m_FilterCoef[FILTERLENGTH];	//narrow band filter coeficients

	//Saved values for filter taps
	FilterElement_t m_IRxFil[FILTERLENGTH];
	FilterElement_t m_QRxFil[FILTERLENGTH];

	//saved state for moving averages used to get from m_SampleRate down to 500 Hz
	FilterElement_t	m_IDecFil1[DECMAX];
	FilterElement_t m_QDecFil1[DECMAX];
	FilterElement_t m_IDecFil2[DECMAX];
	FilterElement_t m_QDecFil2[DECMAX];
	FilterElement_t m_QSum1;
	FilterElement_t m_QSum2;
	FilterElement_t m_ISum1;
	FilterElement_t m_ISum2;

	//saved values for bit synchronization filter taps
	FilterElement_t m_RxAmpFil[BITFILTERLENGTH];

#ifdef _DEBUG
	FilterElement_t		m_PreviousAmpl;
	FilterElement_t				m_PreviousISum;
	FilterElement_t				m_PreviousQSum;
#endif

	void inline ApplyFastFilter(
					FilterElement_t Value,
					FilterElement_t	&Sum1,
					FilterElement_t &Sum2,
					FilterElement_t *Hist1,
					FilterElement_t *Hist2)
	{
		//moving average applied twice
		Sum1 += Value - Hist1[m_DecPtr];
		Hist1[m_DecPtr] = Value;
		Sum2 += Sum1 - Hist2[m_DecPtr];
		Hist2[m_DecPtr] = Sum1;
	}

	//interestingly, the VC++ 5.0 compiler choses NOT to inline this function
	void inline ApplyNarrowFilter(FilterElement_t &ISum, 
									FilterElement_t &QSum)
	{
		int i;
		int fptr = m_RxFilPtr;
		ISum = 0;
		QSum = 0;
		for (i = 0; i < FILTERLENGTH; i += 1)
		{
			ISum += (m_FilterCoef[i] * m_IRxFil[fptr]) ;
			QSum += (m_FilterCoef[i] * m_QRxFil[fptr]) ;
			fptr += 1;
			fptr &= 63;
		}
	}
				

};

#endif 
