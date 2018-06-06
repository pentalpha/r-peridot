#!/bin/bash

echo ""
echo "[ENABLING EPEL]"
echo ""
yum -y install epel-release

echo ""
echo "[UPDATING YUM]"
echo ""
yum check-updates
yum repolist

yum -y install glibc
echo ""
echo "[SETTING LOCALE TO en_US UTF-8]"
echo ""
localedef -c -f UTF-8 -i en_US en_US.UTF-8
export LC_ALL=en_US.UTF-8
echo ""
echo "[INSTALLING JAVA]"
echo ""
yum -y install java-1.8.0-openjdk
echo ""
echo "[INSTALLED JAVA]"
echo ""
echo "[INSTALLING R DEPENDENCIES]"
echo ""
yum install -y wget ca-certificates texlive-tex-gyre make
yum install -y R
yum install -y libcurl libcurl-devel \
libxml2 libxml2-devel \
cairo cairo-devel cairomm-devel \
pango-devel pangomm pangomm-devel
echo ""
echo "[INSTALLED R DEPENDENCIES]"
echo ""