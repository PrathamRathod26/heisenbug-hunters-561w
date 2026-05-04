# syntax=docker/dockerfile:1.7

# --- Stage 1: build the Angular bundle ---
FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Stage 2: serve static files via nginx + reverse-proxy the API ---
FROM nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Angular 19 application builder writes to dist/<project>/browser/
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
