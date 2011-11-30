// PskRxAlgorithms.cpp: implementation of the CPskRxAlgorithms class.
//
//////////////////////////////////////////////////////////////////////

#include "PskRxAlg.h"


#ifdef _DEBUG
#undef THIS_FILE
static char THIS_FILE[]=__FILE__;
#define new DEBUG_NEW
#endif


static const int rxnarrow[64] =		//copied from G3PLX
{ -15, -21, -33, -51, -76,-107,-144,-185,-230,-275,-316,-351,-375,
     -383,-369,-330,-261,-160, -25, 144, 345, 574, 826,1093,1367,1638,
     1896,2130,2331,2490,2600,2656,2656,2600,2490,2331,2130,1896,1638,
     1367,1093, 826, 574, 345, 144, -25,-160,-261,-330,-369,-383,-375,
     -351,-316,-275,-230,-185,-144,-107, -76, -51, -33, -21, -15
};

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CPskRxAlgorithms::CPskRxAlgorithms()
{
	int i;
	m_SineTable = 0;
	for (i = 0; i < FILTERLENGTH; i += 1)
	{
		m_FilterCoef[i] = (FilterElement_t)rxnarrow[i];
		m_IRxFil[i] = 0;
		m_QRxFil[i] = 0;
	}

	for (i = 0; i < BITFILTERLENGTH; i += 1)
	{
		m_RxAmpFil[i] = 0;
	}

	m_Decode = 0;
	m_CenterFrequency = 0;
	m_RxFreq = 0;
	SetCenterFrequency(1000);
	SetSampleRate(11025);
	ResetFilters();

}

CPskRxAlgorithms::~CPskRxAlgorithms()
{

}

void CPskRxAlgorithms::ResetFilters()
{
	int i;
	for (i = 0; i < DECMAX; i += 1)
	{
		m_IDecFil1[i] = 0;
		m_IDecFil2[i] = 0;
		m_QDecFil1[i] = 0;
		m_QDecFil2[i] = 0;
	}
	m_DecPtr = 0;
	m_RxPhase = 0;
	m_DecPhase = 0;
	m_RxBitPhase = 0; 
	m_RxFilPtr = 0;

	m_QSum1 = m_QSum2 = m_ISum1 = m_ISum2 = 0;


	if (m_Decode)
		m_Decode->ResetFilters();

#ifdef _DEBUG
	m_PreviousAmpl = 0;
	m_PreviousISum = 0;
	m_PreviousQSum = 0;
#endif
}

void CPskRxAlgorithms::SetCenterFrequency(long f)
{
	m_CenterFrequency = f;
	AdjustForNewParams();

}

void CPskRxAlgorithms::SetSampleRate(long s)
{
	m_SampleRate = s;
	m_DecSize = (int)((s + 250) / 500);	//number of samples in 500th of a second
	if (s > 0)
	{
		//These are a binary fraction--the binary point is to the left of the MSB
		m_DecFreq = (unsigned short)(0x10000 * (double) 500 / (double) s);
		m_RxBitFreq = (unsigned short)(0x10000 * 31.25 / 500.0);
	}
	AdjustForNewParams();
}

void CPskRxAlgorithms::AdjustForNewParams()
{
	m_RxFreq = 0;
	if (m_SampleRate)
	{
		//Another binary fraction
		m_RxFreq = (unsigned short)(0x10000 * ((double) m_CenterFrequency / (double) m_SampleRate));
	}
}