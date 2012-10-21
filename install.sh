#!/bin/bash
PWD=`pwd`
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR
git pull
git reset HEAD --hard
git submodule update --init
puppet apply --modulepath=$SCRIPT_DIR/puppet/modules $SCRIPT_DIR/puppet/manifests/site.pp
