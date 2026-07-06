

FROM node:20-alpine AS frontend_build
WORKDIR /app

COPY front/package.json front/package-lock.json ./
RUN npm ci

COPY front/ ./

ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=${REACT_APP_API_URL}
RUN npm run build


FROM nginx:1.27-alpine


RUN apk add --no-cache curl

COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf
COPY infra/nginx/site.conf /etc/nginx/conf.d/default.conf

COPY --from=frontend_build /app/build /usr/share/nginx/html

EXPOSE 80 443

HEALTHCHECK --interval=10s --timeout=3s --retries=10 CMD wget -qO- http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
