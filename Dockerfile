FROM clojure:openjdk-8

ADD . /src

RUN curl -sL https://deb.nodesource.com/setup_14.x | bash -
RUN apt-get install -y nodejs

RUN npm install -g typescript
