CC = gcc
CFLAGS = -std=c11 -O1 -fPIC -Wall -Ifirmware

SRC = firmware/pid.c firmware/charger.c firmware/derate.c host/shim.c

lib: libcharger.so

libcharger.so: $(SRC)
	$(CC) $(CFLAGS) -shared -o $@ $(SRC)

test: lib
	python3 -m unittest discover -s tests

clean:
	rm -f libcharger.so
