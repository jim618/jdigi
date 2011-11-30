#include "PskFft.h"

#include <math.h>

const CPskFFT::BitRev[] =
{
   0,0x20,0x10,0x30,0x08,0x28,0x18,0x38,0x04,0x24,0x14,0x34,0x0C,0x2C,0x1C,0x3C,
   2,0x22,0x12,0x32,0x0A,0x2A,0x1A,0x3A,0x06,0x26,0x16,0x36,0x0E,0x2E,0x1E,0x3E,
   1,0x21,0x11,0x31,0x09,0x29,0x19,0x39,0x05,0x25,0x15,0x35,0x0D,0x2D,0x1D,0x3D,
   3,0x23,0x13,0x33,0x0B,0x2B,0x1B,0x3B,0x07,0x27,0x17,0x37,0x0F,0x2F,0x1F,0x3F
};

CPskFFT::CPskFFT()
{
	static const double pi = 3.141592654;
	int i;
	for (i = 0; i < COSTAB_LENGTH + COSTAB_LENGTH/4; i += 1)
	{
		m_SinTab[i] = (float) sin(2*pi*i/FFTSIZE);
	}
}

void CPskFFT::ComputeFFT()
{
	int n2,nu1,i,l,k,pt,p;
	float trl, tim;

	n2 = 32;
	nu1 = 5;
	k=0;
	for (l=1; l <= 6; l += 1)
	{
		pt = 1 << nu1;
		while (k<64)
		{
			for (i=1; i <= n2; i += 1)
			{
				p = BitRev[k / pt];
				trl = m_Ftr[k+n2] * CosTblLookup(p) + 
						m_Fti[k+n2] * SinTblLookup(p);
				tim = m_Fti[k+n2] * CosTblLookup(p) -
						m_Ftr[k+n2] * SinTblLookup(p);
				m_Ftr[k+n2] = m_Ftr[k]-trl;
				m_Fti[k+n2] = m_Fti[k]-tim;
				m_Ftr[k] = m_Ftr[k]+trl;
				m_Fti[k] = m_Fti[k]+tim;
				k += 1;
			}

			k += n2;
		}
		k=0;
		nu1 -= 1;;
		n2 /= 2;
	}
}

float CPskFFT::GetFFTMagnitude(int i)
{
	i = BitRev[i] ^ 1;
	return m_Ftr[i]*m_Ftr[i]+m_Fti[i]*m_Fti[i];
}