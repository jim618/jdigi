// PskTxAlgorithms.h: interface for the CPskTxAlgorithms class.
//
//////////////////////////////////////////////////////////////////////

#ifndef PSKTXALGORITHMS_H
#define PSKTXALGORITHMS_H

#include "SineTab.h"

/* class CPskTxAlgorithms
** This class generates audio samples from a BPSK/QPSK symbol.
** 
** Call the SetNextBit for the next symbol. BPSK symbols are 0 and 2. 
** QPSK uses 1 and 3 in addition.
**
** After calling SetNextBit, then repeated call NextWaveformSample
** to retrieve the audio samples to be transmitted. The "done" argument
** to that function will tell you when you have retrieved the final sample for
** the most recent bit. 
**
**
** The samples are provided in the range of +/- 32000 (approximately)
** which is what PC sound boards expect in their 16 bit digitizing modes.
*/
class CPskTxAlgorithms  
{
public:
	CPskTxAlgorithms();
	~CPskTxAlgorithms();

	//Set the audio center frequency you want generated
	void SetCenterFrequency(long);
	long  GetCenterFrequency(){return m_CenterFrequency;};

	//Set the audio sampling rate your digitizer is using
	void SetSampleRate(long);

	//Force the state the the beginning of a bit
	void Reset();

	//setup the next bit. Do NOT call here until NextWaveformSample tells you "done"
	void SetNextBit(unsigned short b){b %= 5; m_OldBit = m_CurrentBit; m_CurrentBit = b;};

	void SetSineTable(CSineTable *p){m_SineTable = p;};

	//retrieve the next sample in the waveform
	short inline NextWaveformSample(int &done)
	{
		//The sample that returns with done set is still a good sample to be sent
		static const int IVAL[5] = {1, 0, -1, 0, 0};
		static const int QVAL[5] = {0, -1, 0, 1, 0};
		long			OldAmp, NewAmp, Cos;
		long			TxSin, TxCos;
		long			sample;

		//The +1 is because the bit phase is run at HALF the bit rate
		//only half the cosine table is used for the bit rate

		Cos = m_SineTable->CosTblLookup((unsigned short)(m_TxBitPhase >> 17));

		OldAmp = 0x7FFF + Cos;	//Range is 2 * 0x7F00 down to zero
		NewAmp = 0x7FFF - Cos;	//Ditto
		

		TxCos = m_SineTable->CosTblLookup(m_TxPhase);
		TxSin = m_SineTable->SinTblLookup(m_TxPhase);

		sample = (TxCos * ((OldAmp * IVAL[m_OldBit]) +
									(NewAmp * IVAL[m_CurrentBit]))) +
				 (TxSin * ((OldAmp * QVAL[m_OldBit]) +
								(NewAmp * QVAL[m_CurrentBit])));

		sample >>= 16;

		//advance TX phase
		m_TxPhase += m_TxFreq;

		done = AdvanceBitPhase();
		return (short) sample;
	}

protected:
	CSineTable		*m_SineTable;
	void AdjustForNewParams();

	int	inline AdvanceBitPhase()
	{
		unsigned long PrevPhase = m_TxBitPhase;
		m_TxBitPhase += m_TxBitFreq;

		//The compiler provides no access to the processor carry bit.
		//The following detects it. 
		return (m_TxBitPhase < PrevPhase);
	}


	unsigned long	m_TxBitPhase;
	unsigned long	m_TxBitFreq;

	unsigned short	m_TxPhase;
	unsigned short	m_TxFreq;

	long		m_CenterFrequency;
	long		m_SampleRate;

	short	m_OldBit;
	short	m_CurrentBit;
};

#endif 
