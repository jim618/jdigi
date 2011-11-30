#include "PskBitDe.h"  
#include <memory.h>              
                       
#define DIM(X) (sizeof(X)/sizeof(X[0]))

CBpskDecode::CBpskDecode()
{
	memset(m_DecodTab, 0, sizeof(m_DecodTab));
	m_RecCharNotify = 0;
	m_User = 0;
	ResetFilters();
}

void CBpskDecode::SetVaricodeChar(unsigned short Varicode, unsigned short Normal)
{
	Varicode >>= 1;	//dispense with the bottom bit that always set
	if (Varicode < DIM(m_DecodTab))
	{
		m_DecodTab[Varicode] = Normal;
	}
}


void CBpskDecode::VaricodeDecode(int Bit)
{
	m_VarSReg <<= 1;
	if (Bit)
		m_VarSReg |= 1;	//no change in phase is logic 1

	if ((m_VarSReg & 0x7) == 4)
	{
		//end of character
		m_VarSReg >>= 3;	//remove last three bits. they're always 1-0-0
		if (m_VarSReg < DIM(m_DecodTab))
		{
			if (m_RecCharNotify)
				m_RecCharNotify->OnPskCharReceived(m_User, m_DecodTab[m_VarSReg]);

#ifdef _DEBUG
			//build a string we can see in the debugger
			m_Str[m_StrPos++] = (char)m_DecodTab[m_VarSReg];
			m_Str[m_StrPos] = 0;
			if (m_StrPos >= sizeof(m_Str))
				m_StrPos = 0;
#endif
		}
		m_VarSReg = 0;
	}
}

void CBpskDecode::Decode(FilterElement_t ISignal, FilterElement_t QSignal, int Level)
	{

		if (m_RecCharNotify)
			m_RecCharNotify->OnPskSymbol(m_User, ISignal, QSignal);
		//Keep the multiply in range...
		ISignal >>= 16;	QSignal >>= 16;

		FilterElement_t Dot = ISignal * m_OldI + QSignal * m_OldQ;

		VaricodeDecode(Dot > 0.0);
	
		m_OldI = ISignal;
		m_OldQ = QSignal;

	}

void CBpskDecode::ResetFilters()
{
	m_OldI = m_OldQ = 0; 
	m_VarSReg = 0; 	
#ifdef _DEBUG
	m_StrPos = 0;
#endif
};

#ifdef _DEBUG
void CBpskDecode::RetrievePrevious(FilterElement_t &I, FilterElement_t &Q)
{
	I = m_OldI;
	Q = m_OldQ;
}
#endif

//

CQpskDecode::CQpskDecode()
{
	m_reverse = 0;
	ResetFilters();
}


static int distance(int A, int B)	//         {distance=approx euclidean}
{
	static const int dlookup[4][4]=
	{
		{0,2,3,2},
		{2,0,2,3},
		{3,2,0,2},
		{2,3,2,0}};
	return dlookup[A][B];
}

void CQpskDecode::Primetrellis() // {set trellis as if it had been receiving 1's for ever}
{
	int I;
	static const int dprime[16] = {9,10,5,7,7,5,8,9,7,5,7,6,5,4,2,0};

	for (I = 0; I < 16; I += 1)
	{
		trellis[I].dist = dprime[I];
		trellis[I].esti = 0xFFFFFFF0+I;
	}
}

void CQpskDecode::ResetFilters()
{
	CBpskDecode::ResetFilters();
	Primetrellis();
}

void CQpskDecode::Decode(FilterElement_t ISignal, FilterElement_t QSignal, int Level)
{
	static const int rotation[] = {0, 3, 1, 2};

	if (m_RecCharNotify)
			m_RecCharNotify->OnPskSymbol(m_User, ISignal, QSignal);

	//Keep the multiply in range...
	ISignal >>= 17;	QSignal >>= 17;	//16 bits for the multiplies and another for the adds


	long d2 = ISignal * m_OldI + QSignal * m_OldQ;
	long d3 = d2;
	long ay = ISignal * m_OldQ;
	long bx = QSignal * m_OldI;

	d2 = d2 - ay + bx;
	d3 = d3 + ay - bx;

	int 	rcvd = rotation[((d2 >> 30) & 0x2) | ((d3 >> 31) & 0x1)]; 

	m_OldI = ISignal;
	m_OldQ = QSignal;


	static const int depth = 20;

	int i;

    long	dists[32];
    long	ests[32];
	int select,  vote;
	long min;

	if (m_reverse) 
		 rcvd	=	(4-rcvd) & 3;
 

	min = 255;
	for (i = 0; i < 32; i += 1)	//   {calc distances for all states and both data values}
	{
		//{added distance=distance between rcvd and predicted symbol}
		dists[i] = trellis[i >> 1].dist + distance(rcvd, m_Symbols[i]);
		if (dists[i] < min)   
		{
			min = dists[i];	// {keep track of the smallest distance}
		}
		ests[i] = ((trellis[i >> 1].esti) << 1) + (i & 1); // {new estimate}   
	}
	

	for (i = 0; i < 16; i += 1)	// do         {for each new state in the new trellis array}
	{
		if (dists[i] < dists[16+i])
		{
			select = 0;
		}
		else
		{
			select = 16;  //{select lowest}
		}

		trellis[i].dist = dists[select+i] - min;//          {update excess distances}
		trellis[i].esti = ests[select+i] ; //               {keep the new estimate}
	}
	vote = 0;        //                {take a vote of the (depth)th bits}
	for (i = 0; i < 16; i += 1)
	{
		if ((trellis[i].esti & (1L << depth)) > 0) //without the L after the 1, its only 16 bits wide...
		{
			vote++;
		}
	}

	VaricodeDecode(vote<8);
}
