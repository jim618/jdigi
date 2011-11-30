This source code is provided for free, but with out warranty or support. It implements 
the DSP algorithms invented by G3PLX for encoding and decoding PSK signals. This code 
is provided as source code with a test harness. The purpose of the test harness is to 
demonstrate how the C++ classes can be used to generate PSK signals in both BPSK and 
QPSK, and how they can be used to decode PSK signals.

This C++ code is known to have the following portability features/limitations:

1. It REQUIRES that the "long" data type be 32 bits wide.
2. It REQUIRES that the "short" data type be 16 bit wide.
3. The datatype "int" may be either 32 or 16 bits wide.
4. It REQUIRES that the >> operator (right shift) duplicate the
sign bit of signed types. The C++ ANSI standard says that 
right shift of negative values of signed types is "implementation
defined". The attached classes REQUIRE that the implementation
do sign bit duplication.

Integer arithmetic is used throughout. The decoder has been measured at
2% CPU load of a 133Mhz Pentium when running at the Windows standard
sample rate of 11.025 kHz

The classes:

transmitter side:
	CBpskEncode				Takes ASCII, goes through VARICODE to symbols 00, 10
	CQpskEncode				Takes ASCII, goes through VARICODE to symbols 00, 01, 11, 10
	CPskTxAlgorithms		Takes the 00,01,11,10 symbols and makes waveform samples.

receiver side:
	CPskRxAlgorithms		Takes waveform samples and makes 31.25 Hz inphase/quadrature amplitudes
	CBpskDecode				Takes 31.25Hz I/Q amplitudes and makes ASCII
	CQpskDecode				Takes 31.25Hz I/Q amplitudes and makes ASCII
	CPskFFT					does an FFT for the purpose of a tuning indicator

shared helper classes:
	CSineTable				256-entry sin table lookup with linear interpolation
	CConvolutionalSymbols	symbols for the convolutional encoder/viterbi decoder

The files are:

ALPHABET.TXT			varicode mapping
Psk.h					callback/abstract base class definitions.
PskBitDe.cpp
PskBitDe.h
PskEnco.cpp
PskEnco.h
PskFft.cpp
PskFft.h
PskRxAlg.cpp
PskRxAlg.h
Psksimul.exe			DOS executable compiled with MSVC 1.52
Psksimul.mak			MAK file for MSVC 1.52
PskTxAlg.cpp
PskTxAlg.h
SineTab.h

The documentation in the source itself should be enough to figure out what
the classes do. Here is a description of how they plug together:

The connection from CPskRxAlgorithms to its decoder is with a callback through
the abstract base class CPskBitDecode. This keeps the implementation of the 
filtering code separate from the various BPSK/QPSK decoders. The decoders are, 
in turn, connected to the consumer of the ASCII characters with a callback
through the abstract base class CPskRecCharDone.

The separation of implementations on the transmit side is also done through
an abstract base class, CPskBitEncode.

The test program:
PskSimul.cpp is a simple UNIX-style command line parsing test harness that uses
all the above classes. In transmit mode, it takes phrase from the command line
and creates an ASCII text file containing the 11.025KHz sample values generated
to transmit the phrase. In receive mode, it reads file and prints out the
phrase it decodes. You can use the test program to do things like encode at 1000
Hz and decode at 995Hz and see how good your copy is. 

test program file format is one sample per line in digits. The valid range for
a sample is +/-32767. Don't forget this if you edit the sample file as the
decoder is designed to read/write 16 bit samples.

What's not here.
Automatic frequency control is not implemented in these classes.


GL,
Wayne, W5XD