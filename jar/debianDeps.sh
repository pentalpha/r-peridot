#!/bin/bash

apt-get update \
&& apt-get install -y --no-install-recommends locales

echo "[INSTALLING JAVA]"

apt-get update \
	&& apt-get install -y --no-install-recommends \
		openjdk-8-jre

echo "[INSTALLED JAVA]"

echo "[INSTALLING R DEPENDENCIES]"

apt-get update \
	&& apt-get install -y --no-install-recommends \
		wget \
		ca-certificates \
		fonts-texgyre

apt-get update \
	&& apt-get build-dep -y --no-install-recommends r-base-dev

apt-get update \
	&& apt-get install -y \
        r-base \
        r-base-dev \
        build-essential \
        make \
        gcc \
        g++ \
        gfortran \
		libcurl4-openssl-dev \
		libxml2-dev \
		libcairo2 \
		libcairo2-dev \
		libpango1.0-0 \
		libpango1.0-dev

echo "[INSTALLED R DEPENDENCIES]"