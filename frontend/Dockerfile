FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build --configuration=production
RUN find /app/dist -type f | head -30

FROM nginx:1.27-alpine
COPY --from=build /app/dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

ARG BACKEND_HOST=claims-backend.railway.internal
ARG BACKEND_PORT=8080
RUN sed -i "s|BACKEND_PLACEHOLDER|${BACKEND_HOST}:${BACKEND_PORT}|g" \
    /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
