#ifndef SINETABLE_H
#define SINETABLE_H
#include <math.h>   

class CSineTable
{
public:
	enum	{COSINE_TBL_LENGTH = 256,
		COSINE_TBL_BITSIZE = 8};

	CSineTable()
	{
		static const double PI = 3.141592654;
			long i;
			double arg;
			for (i = 0; i < COSINE_TBL_LENGTH; i += 1)
			{
				arg = (i * PI) * 2.0 / COSINE_TBL_LENGTH;
				m_SinTab[i] = (long)((double)0x7FFF * sin(arg));
			}
	}
	
	long inline CosTblLookup(unsigned short i)
			{
			int		Position = ((i + (1 << (COSINE_TBL_BITSIZE - 1))) >> (16 - COSINE_TBL_BITSIZE))
								& ((1 << COSINE_TBL_BITSIZE) - 1);
			short	FineCorrection = (short)(i << COSINE_TBL_BITSIZE);
			long ret = m_SinTab[(Position + (COSINE_TBL_LENGTH/4)) 
								& ((1 << COSINE_TBL_BITSIZE) - 1)];
			long Interpolation = (1608 * FineCorrection) >> 16;
			Interpolation *= m_SinTab[Position];
			Interpolation >>= 16;
			ret -= Interpolation;
			return ret;
			};

	long inline SinTblLookup(unsigned short i)
		{
			int		Position = ((i + (1 << (COSINE_TBL_BITSIZE - 1))) >> (16 - COSINE_TBL_BITSIZE)) 
								& ((1 << COSINE_TBL_BITSIZE) - 1) ;
			short	FineCorrection = (short)(i << COSINE_TBL_BITSIZE);
			long ret = m_SinTab[Position];
			long Interpolation = (1608 * FineCorrection) >> 16;
			Interpolation *= m_SinTab[(Position + (COSINE_TBL_LENGTH/4)) 
								& ((1 << COSINE_TBL_BITSIZE) - 1)];
			Interpolation >>= 16;
			ret += Interpolation;
			return ret;
		};

protected:
	long	m_SinTab[COSINE_TBL_LENGTH + (COSINE_TBL_LENGTH/4)];
};

#endif
