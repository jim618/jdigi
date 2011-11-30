#ifndef PSKBITDECODE_H
#define PSKBITDECODE_H

#include "psk.h"

/*CBpskDecode
**This class is called at 31.25 time/second with the values of the in-phase and
**and quadrature components for the bit to be decoded.
*/
class CBpskDecode : public CPskBitDecode
{
public:
	CBpskDecode();
	virtual void Decode(FilterElement_t ISignal, FilterElement_t QSignal, int Level);
	virtual void ResetFilters();

	//This class does varicode decoding. But it doesn't know the Varicode. You 
	//have to teach varicode by calling SetVaricodeChar for each character in
	//the alphabet
	void SetVaricodeChar(unsigned short Varicode, unsigned short Normal);

	//This class will forward the decoded character to a CPskRecCharDone if
	//you call here to set it up.
	void SetReceivedNotification(short user, CPskRecCharDone *p)
			{m_User = user;m_RecCharNotify = p;};

#ifdef _DEBUG
	virtual void RetrievePrevious(FilterElement_t &, FilterElement_t &);
#endif


protected:
	void VaricodeDecode(int);
	short					m_User;
	unsigned short			m_VarSReg;
	FilterElement_t			m_OldI, m_OldQ;
	unsigned short			m_DecodTab[2047];
	CPskRecCharDone			*m_RecCharNotify;

#ifdef _DEBUG
	char	m_Str[1024];
	int		m_StrPos;
#endif
};

class CQpskDecode : public CBpskDecode
{
public:
	CQpskDecode();
	void SetReverse(int rev){m_reverse = rev;}
	
	virtual void ResetFilters();
	virtual void Decode(FilterElement_t ISignal, FilterElement_t QSignal, int Level);

protected:
	CConvolutionalSymbols	m_Symbols;
	void Primetrellis();
	int	m_reverse;
	struct {
			long	 dist;
			long	 esti;
			}
			trellis[16];
};


#endif          
