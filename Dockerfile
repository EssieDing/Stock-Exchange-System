# pull from base image
FROM gradle:7.4.1-jdk17

# define working directory
RUN mkdir /code
WORKDIR /code

# add all files
ADD . /code

RUN chmod +x scripts/runserver.sh
RUN gradle assemble