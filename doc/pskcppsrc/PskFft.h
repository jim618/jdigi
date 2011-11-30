#ifndef PSKFFT_H
#define PSKFFT_H

#include "psk.h"

class CPskFFT
{
public:
	CPskFFT();
	void SetIrxQrx(int i, FilterElement_t Irx, FilterElement_t Qrx)
	{
		m_Ftr[i] = (float)(Irx * (1.0 - CosTblLookup(i)));
		m_Fti[i] = (float)(Qrx * (1.0 - CosTblLookup(i)));
	}

	void ComputeFFT();

	float	GetFFTMagnitude(int i);


protected:
	enum {COSTAB_LENGTH = 64, FFTSIZE = 64};
	static const int BitRev[FFTSIZE];
	float inline CosTblLookup(int i){return m_SinTab[i + COSTAB_LENGTH/4];};
	float inline SinTblLookup(int i){return -m_SinTab[i];};

	float	m_Ftr[FFTSIZE];
	float	m_Fti[FFTSIZE];
	float	m_SinTab[COSTAB_LENGTH + (COSTAB_LENGTH / 4)];	
};

#endif
