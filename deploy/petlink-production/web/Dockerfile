FROM docker.m.daocloud.io/library/nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY pc/ /usr/share/nginx/html/
COPY mobile/ /usr/share/nginx/html/mobile/
