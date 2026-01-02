import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "export",
  images: { unoptimized: true }, // needed for static export if you use next/image
};

export default nextConfig;