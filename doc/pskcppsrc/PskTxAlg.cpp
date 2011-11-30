// PskTxAlgorithms.cpp: implementation of the CPskTxAlgorithms class.
//
//////////////////////////////////////////////////////////////////////

#include <math.h>
#include "PskTxAlg.h"

#ifdef _DEBUG
#undef THIS_FILE
static char THIS_FILE[]=__FILE__;
#define new DEBUG_NEW
#endif


//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////


CPskTxAlgorithms::CPskTxAlgorithms()
{
	Reset();
	SetCenterFrequency(1000);
	SetSampleRate(11025);
	m_SineTable = 0;

}

CPskTxAlgorithms::~CPskTxAlgorithms()
{

}

void CPskTxAlgorithms::Reset()
{
	m_TxPhase = 0;
	m_TxBitPhase = 0;

	m_OldBit = 4;
	m_CurrentBit = m_OldBit;

}

void CPskTxAlgorithms::SetCenterFrequency(long f)
{
	m_CenterFrequency = f;
	AdjustForNewParams();
}

void CPskTxAlgorithms::SetSampleRate(long s)
{
	m_SampleRate = s;
	AdjustForNewParams();
}

void CPskTxAlgorithms::AdjustForNewParams()
{
	m_TxFreq = 0;
	m_TxBitFreq = 0;
	if (m_SampleRate)
	{
		// binary fraction
		m_TxFreq = (unsigned short)(0x10000 * ((double) m_CenterFrequency / (double) m_SampleRate));
		m_TxBitFreq = (unsigned long)((double) 0x10000 * 0x10000 * 31.25 / (double) m_SampleRate);
	}
}