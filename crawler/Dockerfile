FROM mcr.microsoft.com/playwright:v1.64.1-noble AS build
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY tsconfig.json ./
COPY src ./src
RUN npm run build

FROM mcr.microsoft.com/playwright:v1.64.1-noble AS runtime
ENV NODE_ENV=production
ENV TZ=UTC
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
COPY app.json ./

USER pwuser

CMD ["node", "dist/main.js"]
