# Stage 1 - the build process
FROM node:10-alpine as build-deps

WORKDIR /app

# Install dependencies (speeds up rebuilding)
COPY package.json yarn.lock ./
RUN yarn


# Stage 2 - tests
FROM build-deps as tests

# Copy source codes
COPY . ./

# Add config file from env mode build arg
# We can't use AWS machine environment variables to select config variant
# (as in scala case) because no ENV var can be passed after yarn build.
# Consequence is that we will need to build separate images for stagin/production
# environment.
ARG ENV_MODE=staging
COPY ./$ENV_MODE.env /app/.env

# Run tests & build app
RUN CI=true yarn test --colors && \
  yarn build --colors



# Stage 3 - the production environment
FROM nginx:1.13.9-alpine

# Nginx config
RUN rm -rf /etc/nginx/conf.d
COPY conf /etc/nginx

# Copy built web from previous stage (source codes will be forgotten)
COPY --from=tests /app/build /usr/share/nginx/html

# Run server on port 80
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]