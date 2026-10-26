import type {NextConfig} from "next";

const nextConfig: NextConfig = {
    output: "standalone",
};

export default nextConfig;

module.exports = {
    allowedDevOrigins: ['127.0.0.1'],
}