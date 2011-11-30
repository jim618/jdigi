# Microsoft Visual C++ generated build script - Do not modify

PROJ = PSKSIMUL
DEBUG = 0
PROGTYPE = 6
CALLER = 
ARGS = 
DLLS = 
D_RCDEFINES = -d_DEBUG
R_RCDEFINES = -dNDEBUG
ORIGIN = MSVC
ORIGIN_VER = 1.00
PROJPATH = F:\HAMSRC\PSKCTRL\DOS2\
USEMFC = 0
CC = cl
CPP = cl
CXX = cl
CCREATEPCHFLAG = 
CPPCREATEPCHFLAG = 
CUSEPCHFLAG = 
CPPUSEPCHFLAG = 
FIRSTC =             
FIRSTCPP = PSKBITDE.CPP
RC = rc
CFLAGS_D_DEXE = /nologo /G2 /W3 /Zi /AM /Od /D "_DEBUG" /D "_DOS" /FR /Fd"PSKSIMUL.PDB"
CFLAGS_R_DEXE = /nologo /Gs /G3 /W3 /AM /O2 /Ox /Ob2 /D "NDEBUG" /D "_DOS" /FR 
LFLAGS_D_DEXE = /NOLOGO /ONERROR:NOEXE /NOI /CO /STACK:5120
LFLAGS_R_DEXE = /NOLOGO /ONERROR:NOEXE /NOI /STACK:5120
LIBS_D_DEXE = oldnames mlibce
LIBS_R_DEXE = oldnames mlibce
RCFLAGS = /nologo
RESFLAGS = /nologo
RUNFLAGS = 
OBJS_EXT = 
LIBS_EXT = 
!if "$(DEBUG)" == "1"
CFLAGS = $(CFLAGS_D_DEXE)
LFLAGS = $(LFLAGS_D_DEXE)
LIBS = $(LIBS_D_DEXE)
MAPFILE = nul
RCDEFINES = $(D_RCDEFINES)
!else
CFLAGS = $(CFLAGS_R_DEXE)
LFLAGS = $(LFLAGS_R_DEXE)
LIBS = $(LIBS_R_DEXE)
MAPFILE = nul
RCDEFINES = $(R_RCDEFINES)
!endif
!if [if exist MSVC.BND del MSVC.BND]
!endif
SBRS = PSKBITDE.SBR \
		PSKENCO.SBR \
		PSKFFT.SBR \
		PSKRXALG.SBR \
		PSKSIMUL.SBR \
		PSKTXALG.SBR


PSKBITDE_DEP = f:\hamsrc\pskctrl\dos2\pskbitde.h \
	f:\hamsrc\pskctrl\dos2\psk.h


PSKENCO_DEP = f:\hamsrc\pskctrl\dos2\pskenco.h \
	f:\hamsrc\pskctrl\dos2\psk.h


PSKFFT_DEP = f:\hamsrc\pskctrl\dos2\pskfft.h \
	f:\hamsrc\pskctrl\dos2\psk.h


PSKRXALG_DEP = f:\hamsrc\pskctrl\dos2\pskrxalg.h \
	f:\hamsrc\pskctrl\dos2\psk.h \
	f:\hamsrc\pskctrl\dos2\pskbitde.h \
	f:\hamsrc\pskctrl\dos2\sinetab.h


PSKSIMUL_DEP = f:\hamsrc\pskctrl\dos2\psk.h \
	f:\hamsrc\pskctrl\dos2\pskbitde.h \
	f:\hamsrc\pskctrl\dos2\pskenco.h \
	f:\hamsrc\pskctrl\dos2\psktxalg.h \
	f:\hamsrc\pskctrl\dos2\sinetab.h \
	f:\hamsrc\pskctrl\dos2\pskrxalg.h \
	f:\hamsrc\pskctrl\dos2\pskfft.h


PSKTXALG_DEP = f:\hamsrc\pskctrl\dos2\psktxalg.h \
	f:\hamsrc\pskctrl\dos2\sinetab.h


all:	$(PROJ).EXE $(PROJ).BSC

PSKBITDE.OBJ:	PSKBITDE.CPP $(PSKBITDE_DEP)
	$(CPP) $(CFLAGS) $(CPPCREATEPCHFLAG) /c PSKBITDE.CPP

PSKENCO.OBJ:	PSKENCO.CPP $(PSKENCO_DEP)
	$(CPP) $(CFLAGS) $(CPPUSEPCHFLAG) /c PSKENCO.CPP

PSKFFT.OBJ:	PSKFFT.CPP $(PSKFFT_DEP)
	$(CPP) $(CFLAGS) $(CPPUSEPCHFLAG) /c PSKFFT.CPP

PSKRXALG.OBJ:	PSKRXALG.CPP $(PSKRXALG_DEP)
	$(CPP) $(CFLAGS) $(CPPUSEPCHFLAG) /c PSKRXALG.CPP

PSKSIMUL.OBJ:	PSKSIMUL.CPP $(PSKSIMUL_DEP)
	$(CPP) $(CFLAGS) $(CPPUSEPCHFLAG) /c PSKSIMUL.CPP

PSKTXALG.OBJ:	PSKTXALG.CPP $(PSKTXALG_DEP)
	$(CPP) $(CFLAGS) $(CPPUSEPCHFLAG) /c PSKTXALG.CPP

$(PROJ).EXE::	PSKBITDE.OBJ PSKENCO.OBJ PSKFFT.OBJ PSKRXALG.OBJ PSKSIMUL.OBJ PSKTXALG.OBJ $(OBJS_EXT) $(DEFFILE)
	echo >NUL @<<$(PROJ).CRF
PSKBITDE.OBJ +
PSKENCO.OBJ +
PSKFFT.OBJ +
PSKRXALG.OBJ +
PSKSIMUL.OBJ +
PSKTXALG.OBJ +
$(OBJS_EXT)
$(PROJ).EXE
$(MAPFILE)
D:\MSVC\LIB\+
D:\MSVC\MFC\LIB\+
D:\MTX\LIB\+
$(LIBS)
$(DEFFILE);
<<
	link $(LFLAGS) @$(PROJ).CRF

run: $(PROJ).EXE
	$(PROJ) $(RUNFLAGS)


$(PROJ).BSC: $(SBRS)
	bscmake @<<
/o$@ $(SBRS)
<<
