#ifndef PSK_H
#define PSK_H

//This file contains the virtual base classes used to connect the various parts
//of the PSK receiver together. 

typedef long FilterElement_t;	//"float" and "double" also work, but are a bit slower

//This class is called at 31.25 baud with the I/Q components of the signal.
class CPskBitDecode
{
public:
	virtual void Decode(FilterElement_t ISignal, FilterElement_t QSignal, int Level) = 0;
	virtual void ResetFilters()=0;
#ifdef _DEBUG
	virtual void RetrievePrevious(FilterElement_t &, FilterElement_t &) = 0;
#endif
};

//This class is also called at 31.25 baud, but has the result of having done the
//varicode translation and either the BPSK or QPSK demodulation.
class CPskRecCharDone
{
public:
	virtual void OnPskCharReceived(short User, short ReceivedChar)=0;
	virtual void OnPskSymbol(short User, FilterElement_t ISignal, FilterElement_t QSignal)=0;
};


class CPskBitEncode
{
public:
	virtual void SetVaricodeChar(unsigned short Varicode, unsigned short Normal)=0;
	virtual void SetNextChar(unsigned short Next)=0;
	virtual void Reset()=0;
	
	//When done returns set the symbol IS to be sent, but no more
	virtual int	 NextWaveformSymbol(int &done)=0;
};


//utility functions


//Class to construct the convolutional symbols
class CConvolutionalSymbols
{
public:
	CConvolutionalSymbols()
	{
		int i;
		for (i = 0; i < 32; i += 1)
		{
			m_Symbols[i] = ConvolutionalSymbol(i);
		}
	}
	operator [](int i)
	{
		return m_Symbols[i];
	}

protected:
	enum {poly1 = 0x19, poly2 = 0x17};       //{convolutional encoder polynomials}
	int m_Symbols[32];
	int Parity(unsigned int data)
	{
		int count = 0;

		while (data>0)
		{
			if (data & 1)
			{
				count++;
			}
			data >>= 1;
		}
		return count & 1;
	}

	int ConvolutionalSymbol(int i){return (Parity(i & poly1) << 1) | Parity(i & poly2);}

};

#endif
