.DEFAULT_GOAL := build-run

clean:
		make -C app clean

build:
		make -C app build

install:
		make -C app install

test:
		make -C app test

lint:
		make -C app lint

report:
		make -C app report

update-deps:
		make -C app update-deps

build-run:
		make -C app build run

.PHONY: build