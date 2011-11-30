// BpskEncode.cpp: implementation of the CBpskEncode class.
//
//////////////////////////////////////////////////////////////////////

#include "PskEnco.h"
#include <memory.h>        

#define DIM(X) (sizeof(X)/sizeof(X[0]))

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CBpskEncode::CBpskEncode()
{
	memset(&m_EncodTab, 0, sizeof(m_EncodTab));
	m_PreviousSymbol = 0;
	Reset();
}

CBpskEncode::~CBpskEncode()
{
}

void CBpskEncode::Reset()
{
	m_CurrentChar = 0;
}

void CBpskEncode::SetVaricodeChar(unsigned short Varicode, unsigned short Normal)
{
	short temp;
	if (Normal < DIM(m_EncodTab))
	{
		//The code is to be transmitted the MOST significant ONE bit first.
		//So shift the code left until a one is on top
		temp = (short) Varicode;
		if (temp)
		{
			while (temp > 0)
				temp <<= 1;
		}
		else
			temp = (short)0x8000;

		//now there's a one on top

		temp >>= 2;		//make room for two zero bits and the one bit to start...
		//Every transmitted character begins with a pair of zero bits and a one bit and then
		//the "data" bits of the varicode
		m_EncodTab[Normal] = (unsigned short) (temp & 0x3FFF);
	}
}

void CBpskEncode::SetNextChar(unsigned short Next)
{
	if (Next < DIM(m_EncodTab))
	{
		m_CurrentChar = m_EncodTab[Next];
	}
}


int CBpskEncode::NextWaveformSymbol(int &done)
{
	int	RetVal = 	((NextBit() << 1) + m_PreviousSymbol) & 3;

	m_PreviousSymbol = RetVal;

	done = (m_CurrentChar == 0);
	
	return RetVal;
}

//QPSK encoder


CQpskEncode::CQpskEncode()
{
	m_reverse = 0;
	Reset();
}

CQpskEncode::~CQpskEncode()
{
}

void CQpskEncode::Reset()
{
	m_Convol = 0;
}

int	 CQpskEncode::NextWaveformSymbol(int &done)
{
	int b = NextBit();

	m_Convol = ((m_Convol << 1) + (b & 1)) & 0x1F;

	int RetVal = m_Symbols[m_Convol];

	if (m_reverse)
		RetVal = 4 - RetVal;

	//The "RetVal" at this point matches rcvd in CQpskDecode::Decode 

	RetVal = (RetVal + m_PreviousSymbol) & 3;
	m_PreviousSymbol = RetVal;

	done = (m_CurrentChar == 0);
	return RetVal;
}

CQpskRevEncode::CQpskRevEncode()
{
	m_reverse = 1;
}