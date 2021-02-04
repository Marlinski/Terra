#!/bin/bash

./gradlew build && ./gradlew distTar

if [ $? -ne 0 ]
then
  echo "Could not create file" >&2
  exit 1
fi

# collect all jar and binaries to deploy
rm -rf ./linux-dtn
mkdir linux-dtn
mkdir linux-dtn/bin
mkdir linux-dtn/lib
mkdir linux-dtn/modules
mkdir linux-dtn/storage

tar xvf linux/terra/build/distributions/terra.tar -C linux-dtn/
mv linux-dtn/terra/bin/* linux-dtn/bin
mv linux-dtn/terra/lib/* linux-dtn/lib
rm -rf linux-dtn/terra/

tar xvf linux/dtnping/build/distributions/dtnping.tar -C linux-dtn/
mv linux-dtn/dtnping/bin/* linux-dtn/bin
mv linux-dtn/dtnping/lib/* linux-dtn/lib
rm -rf linux-dtn/dtnping/

tar xvf linux/dtncat/build/distributions/dtncat.tar -C linux-dtn/
mv linux-dtn/dtncat/bin/* linux-dtn/bin
mv linux-dtn/dtncat/lib/* linux-dtn/lib
rm -rf linux-dtn/dtncat/

cp modules/cla/libdtn-module-stcp/build/libs/libdtn-module-stcp.jar linux-dtn/modules/
cp modules/core/libdtn-module-hello/build/libs/libdtn-module-hello.jar linux-dtn/modules/
cp modules/core/libdtn-module-ipdiscovery/build/libs/libdtn-module-ipdiscovery.jar linux-dtn/modules/
cp modules/core/libdtn-module-http/build/libs/libdtn-module-http.jar linux-dtn/modules/

# tar
tar cvzf ./linux-dtn.tar.gz ./linux-dtn/
#rm -rf linux-dtn/

