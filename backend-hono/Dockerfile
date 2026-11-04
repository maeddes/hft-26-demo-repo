FROM node:24-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY index.ts ./
USER node
EXPOSE 8080
CMD ["node", "index.ts"]
