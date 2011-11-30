// BpskEncode.h: interface for the CBpskEncode class.
//
//////////////////////////////////////////////////////////////////////

#ifndef BPSKENCODE_H
#define BPSKENCODE_H

#include "psk.h"

class CBpskEncode : public CPskBitEncode 
{
public:
	CBpskEncode();
	virtual ~CBpskEncode();

	void SetVaricodeChar(unsigned short Varicode, unsigned short Normal);
	void SetNextChar(unsigned short Next);
	void Reset();
	
	//When done returns set the symbol IS to be sent, but no more
	int	 NextWaveformSymbol(int &done);

protected:
	int CBpskEncode::NextBit()
	{
		int retval = ((m_CurrentChar >> 15) ^ 1);	//pick up top bit and invert it
		m_CurrentChar <<= 1;
		return retval;
	}

	unsigned short m_EncodTab[256];
	unsigned short m_CurrentChar;
	unsigned short m_PreviousSymbol;
};


class CQpskEncode : public CBpskEncode 
{
public:
	CQpskEncode();
	virtual ~CQpskEncode();

	void Reset();
	
	//When done returns set the symbol IS to be sent, but no more
	int	 NextWaveformSymbol(int &done);

protected:
	CConvolutionalSymbols	m_Symbols;
	int		m_reverse;
	char	m_Convol;
};

class CQpskRevEncode : public CQpskEncode
{
public:
	CQpskRevEncode();
};

#endif 
