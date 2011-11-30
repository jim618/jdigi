#include <stdio.h>
#include <stdlib.h>

#include "psk.h"
#include "pskbitde.h"
#include "pskenco.h"
#include "psktxalg.h"
#include "pskrxalg.h"
#include "pskfft.h"

enum Modulation_t {BPSK, QPSK, QPSK_REVERSE};


static void DoTransmission(const char *, const char *, int , enum		Modulation_t);
static void DoReception(const char *, int, enum Modulation_t);

FILE *DbgOutFile;

int main(int argc, char **argv)
{
	const char *Fname = "PSK.TXT";
	const char *PhraseToSend = "This is a test.";
	int			CenterFrequency = 1000;
	enum		Modulation_t Modulation = BPSK;
	int			TransmitFlag = 0;
	long i;

 
	if (argc < 2)
	{
		puts("usage:\n"
				"PskSimulator -x[B,Q,QR] \"text to code\" -f FileToCreate -c nnnn\n"
				"\twhere nnnn is the center frequency\n"
				"or\n"
				"PskSimulator -r[B,Q,QR] -f FileToRead -c nnnn\n"
				"\twhere B is BPSK, Q is QPSK, and QR is QPSK reverse\n"
				"\nThis program writes and read waveforms as ASCII text with a sample\n"
				"on each line of the file. The sample must be an integer in the range\n"
				"of +/- 32000\n"				
				);
		return 0;
	}

	//An old-fashioned, UNIX-style test program: parse the command line arguments...
	for (i = 1; i < argc; i += 1)
	{
		if (argv[i] && (argv[i][0] == '-'))
		{
			switch (argv[i][1])
			{
			case 'x':
			case 'X':
				TransmitFlag = 1;
			case 'r':
			case 'R':
				switch (argv[i][2])
				{
				case 'q':
				case 'Q':
					switch(argv[i][3])
					{
						case 0:
							Modulation = QPSK;
							break;
						case 'r':
						case 'R':
							Modulation = QPSK_REVERSE;
							break;
						default:
							puts("Bad argument: "); puts(argv[i]); puts("\n");
							break;
					}
					break;
				case 'B':
				case 'b':
				case 0:
					break;

				default:
					printf("Bad argument: \"%s\"\n",argv[i]);
					break;

				}
				if (TransmitFlag)
				{
					i += 1;
					if (i < argc)
					{
						PhraseToSend = argv[i];
					}
					else
					{
					}
				}
				break;

			case 'f':
			case 'F':
				i += 1;
				if (i < argc)
				{
					Fname = argv[i];
				}
				else
				{
					puts("No file name provided");
				}
				break;

			case 'c':
			case 'C':
				i += 1;
				if (i < argc)
				{
					CenterFrequency = atoi(argv[i]);
				}
				else
				{
					printf("No center frequency provided\n");
				}
				break;

			default:
				printf("Bad argument: \"%s\"\n", argv[i]);
				break;
			}
		}
	}

	//display console messages indicating what we're going to do
	if (TransmitFlag)
	{
		printf("Simulating transmission of the phrase: \"%s\" into file: \"%s\"\n",
						PhraseToSend, Fname);
	}
	else
	{
		printf("Simulating reception from file: \"%s\"\n", Fname);
	}

	//Display the modulation type
	printf("Modulation mode is: ");
	switch (Modulation)
	{
	case BPSK:
		puts("BPSK");
		break;
	case QPSK:
		puts("QPSK");
		break;
	case QPSK_REVERSE:
		puts("QPSK(reverse)");
		break;
	}

	//display the center frequency to be used
	printf("Center frequency is: %d\n", CenterFrequency);

	//do the transmission or reception as indicated by the command line arguments
	if (TransmitFlag)
	{
		DoTransmission(Fname, PhraseToSend, CenterFrequency, Modulation);
	}
	else
	{
		DoReception(Fname, CenterFrequency, Modulation);
	}
	
	return 0;
}

static void InitVaricode(CBpskDecode *pDecode, CPskBitEncode *pEncode, const char *pFname)
{
	char	Buf[128];
	long	l;
	int		i;
	char	*p;	//dummy

	FILE *f = fopen("alphabet.txt", "r");
	if (f)
	{
		fgets(Buf, sizeof(Buf), f);	//skip first line
		i = 0;
		while (fgets(Buf, sizeof(Buf), f))
		{
			l =	strtoul(Buf, &p, 2);
			if (pDecode)
				pDecode->SetVaricodeChar((unsigned short)l, i);
			if (pEncode)
				pEncode->SetVaricodeChar((unsigned short)l, i);
			i += 1;
		}
		fclose(f);
	}
	else
	{
		printf("CAN'T FIND ALPHABET.TXT!\n");
	}
}

static void BitsToFile(FILE *f, CPskTxAlgorithms *pTx, int NextSymbol)
{
	int BitDone = 0;
	pTx->SetNextBit(NextSymbol);
	while (!BitDone)
	{
		fprintf(f, "%d\n", pTx->NextWaveformSample(BitDone));
	}
}

static void DoTransmission(const char *File, const char *pText, int Frequency, enum Modulation_t Modulation)
{
	CPskTxAlgorithms	PskTxAlgorithms;
	CBpskEncode		*pPskEncode = 0;
	int					CharDone;
	int					NextSymbol;
	int					i;
	CSineTable			t;

	PskTxAlgorithms.SetSineTable(&t);

	FILE *f;

	switch (Modulation)
	{
	case BPSK:
		pPskEncode = new CBpskEncode();
		break;
	case QPSK:
		pPskEncode = new CQpskEncode();
		break;
	case QPSK_REVERSE:
		pPskEncode = new CQpskRevEncode();
		break;
	}

	if (!pPskEncode)
		return;

	f = fopen(File, "w");
	if (f)
	{

		InitVaricode(0, pPskEncode,  "alphabet.txt");

		PskTxAlgorithms.SetCenterFrequency(Frequency);
		PskTxAlgorithms.SetSampleRate(11025);

		//Carrier
		for (i = 0; i < 4; i += 1)
		{
			BitsToFile(f, &PskTxAlgorithms, 0);
		}
		//then idle a few bits
		for (i = 0; i < 4; i += 1)
		{
			NextSymbol = pPskEncode->NextWaveformSymbol(CharDone);
			BitsToFile(f, &PskTxAlgorithms, NextSymbol);
		}


		while (*pText)
		{
			CharDone = 0;
			pPskEncode->SetNextChar(*pText++);

			while (!CharDone)
			{
				NextSymbol = pPskEncode->NextWaveformSymbol(CharDone);
				BitsToFile(f, &PskTxAlgorithms, NextSymbol);
			}
		}

		//A few more idle bits at the end
		for (i = 0; i < 4; i += 1)
		{
			NextSymbol = pPskEncode->NextWaveformSymbol(CharDone);
			BitsToFile(f, &PskTxAlgorithms, NextSymbol);
		}

		BitsToFile(f, &PskTxAlgorithms, 4);	//OFF

		fclose(f);
	}
	else
	{
		printf("Failed to open: %s\n", File);
	}
	delete pPskEncode;

}

class CSimulatorReceiver : public CPskRecCharDone
{
public:
	CSimulatorReceiver()
	{
		m_OldI = m_OldQ = 0;
	}
		void OnPskSymbol(short, long i,long q)
		{
			double at1 = atan2(i, q);
			double at2 = atan2(m_OldI, m_OldQ);
			at1 -= at2;
			at1 *= 180 / 3.141592654;
			m_OldI = i;
			m_OldQ = q;
		};
		void OnPskCharReceived(short, short c)
		{
			putchar(c);
		};
protected:
	long m_OldI;
	long m_OldQ;

};


class CSimulatorDecode : public CPskBitDecode
{
public:
	CSimulatorDecode()
	{
		m_Decode = 0;
		m_Rx = 0;
	}

	void Decode(FilterElement_t ISignal, FilterElement_t QSignal, int Level)
	{
		int i;
		FilterElement_t I, Q;
		for (i = 0; i < 64; i += 1)
		{
			m_Rx->GetIrxQrx(i, I, Q);
			m_FFt.SetIrxQrx(i, I, Q);
		}
		m_FFt.ComputeFFT();
		m_Decode->Decode(ISignal, QSignal, Level);
	}


	void ResetFilters()
	{
		m_Decode->ResetFilters();
	};
#ifdef _DEBUG
	void RetrievePrevious(FilterElement_t &I, FilterElement_t &Q)
	{
		m_Decode->RetrievePrevious(I, Q);
	}
#endif

	void SetDecode(CPskBitDecode *p){m_Decode = p;}
	void SetRx(CPskRxAlgorithms *p){m_Rx = p;}
protected:
	CPskBitDecode		*m_Decode;
	CPskRxAlgorithms	*m_Rx;
	CPskFFT				m_FFt;
};



static void DoReception(const char * Fname, int Freq, enum Modulation_t Modulation)
{
	char	Buf[128];
	CPskRxAlgorithms	PskRxAlgorithms;
	CSineTable			SineTable;
	CBpskDecode			*pPskDecode = 0;
	CQpskDecode			*pQpsk;
	CSimulatorReceiver	Receiver;
	CSimulatorDecode	SimDecode;
	int					rev = 0;


	PskRxAlgorithms.SetSineTable(&SineTable);

	switch (Modulation)
	{
	case BPSK:
		pPskDecode = new CBpskDecode();
		break;

	case QPSK_REVERSE:
		rev = 1;
	case QPSK:
		pQpsk = new CQpskDecode();
		pQpsk->SetReverse(rev);
		pPskDecode = pQpsk;
		pQpsk = 0;	//don't use anymore...
		break;
	}

	if (!pPskDecode)
		return;

	SimDecode.SetDecode(pPskDecode);
	SimDecode.SetRx(&PskRxAlgorithms);
	PskRxAlgorithms.SetDecoder(&SimDecode);
	PskRxAlgorithms.SetSampleRate(11025);
	PskRxAlgorithms.SetCenterFrequency(Freq);

	pPskDecode->SetReceivedNotification(0, &Receiver);

	InitVaricode(pPskDecode, 0, "alphabet.txt");

	FILE *f = fopen(Fname, "r");
	if (f)
	{
#ifdef _DEBUG
		DbgOutFile = fopen("DbgOut.txt", "w");
#endif
		while (fgets( Buf, sizeof(Buf), f))
		{
			short Sample = atoi(Buf);
			PskRxAlgorithms.AddRxSample(Sample);
		}
		fclose(f);
	}
	else
	{
		printf("Can't open %s for read\n", Fname);
	}

	delete pPskDecode;  
	
	puts("\nReception complete");

}
