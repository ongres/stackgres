#!/bin/bash


MAJOR_VERSION=18
MINOR_VERSION=1
PG_VERSION=${MAJOR_VERSION}.${MINOR_VERSION}
PG_BIN_DIR=${HOME}/postgresql-${MAJOR_VERSION}/bin

# Public postgresql.conf from https://postgresqlco.nf
# You may use your own
POSTGRESQLCO_NF_UUID=5c9150db-bbec-4c3d-9e42-cfe64c2d3d42


# Install compile dependencies
sudo yum -y install bzip2 gcc bison flex perl-FindBin perl-File-Compare

# Download, configure, compile and install PostgreSQL
mkdir src
cd src
curl -s https://ftp.postgresql.org/pub/source/v${PG_VERSION}/postgresql-${PG_VERSION}.tar.bz2 \
	| tar xjf -
cd postgresql-${PG_VERSION}
./configure \
	--prefix=${HOME}/postgresql-${MAJOR_VERSION} \
	--without-zlib \
	--without-readline \
	--without-icu
make -j2 install
cd
rm -rf src

# Set path in .bashrc, add database start command
cat << EOF >> .bashrc

# PostgreSQL on CloudShell
export PATH=${PG_BIN_DIR}:\$PATH

# Ensure we can source .bashrc in an essentially idempotent manner
( ${PG_BIN_DIR}/pg_ctl -D pgdata status > /dev/null ) || ${PG_BIN_DIR}/pg_ctl -D pgdata start
#

EOF

# Initialize the database files directory ($PGDATA)
${PG_BIN_DIR}/initdb -E UTF-8 -D pgdata \
	--data-checksums \
	--auth-host=md5 --auth-local=peer

# Fetch postgresql.conf from reference https://postgresqlco.nf configuration
mv pgdata/postgresql.conf pgdata/postgresql.conf.orig
curl -s -o pgdata/postgresql.conf \
	'https://api.postgresqlco.nf/api/v1/public/configs/'${POSTGRESQLCO_NF_UUID}'/postgresql.conf/export?format=default'

# Start the database
${PG_BIN_DIR}/pg_ctl -D pgdata start

# Create initial database user, so we can use Postgres simply executing "psql"
${PG_BIN_DIR}/createdb "${USER}"
