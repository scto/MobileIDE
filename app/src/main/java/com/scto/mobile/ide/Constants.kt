package com.scto.mobile.ide

object Constants {
    private const val BASE_URL = "https://raw.githubusercontent.com/scto/Karbon-PackagesX/main"
    const val PROOT_ARM = "$BASE_URL/arm/proot"
    const val PROOT_ARM64 = "$BASE_URL/aarch64/proot"
    const val PROOT_X64 = "$BASE_URL/x86_64/proot"
    const val TALLOC_ARM = "$BASE_URL/arm/libtalloc.so.2"
    const val TALLOC_ARM64 = "$BASE_URL/aarch64/libtalloc.so.2"
    const val TALLOC_X64 = "$BASE_URL/x86_64/libtalloc.so.2"

    /* Ubuntu */
    private const val UBUNTU_ROOTFS_BASE = "https://github.com/Xed-Editor/Karbon-PackagesX/releases/download/ubuntu"
    const val UBUNTU_ARM = "$UBUNTU_ROOTFS_BASE/ubuntu-base-24.04.3-base-armhf.tar.gz"
    const val UBUNTU_ARM64 = "$UBUNTU_ROOTFS_BASE/ubuntu-base-24.04.3-base-arm64.tar.gz"
    const val UBUNTU_X64 = "$UBUNTU_ROOTFS_BASE/ubuntu-base-24.04.3-base-amd64.tar.gz"

    /* Debian */
    private const val DEBIAN_ROOTFS_BASE = "https://github.com/Xed-Editor/Karbon-PackagesX/releases/download/debian"
    const val DEBIAN_ARM = "$DEBIAN_ROOTFS_BASE/debian-rootfs-amdhf.tar.xz"
    const val DEBIAN_X64 = "$DEBIAN_ROOTFS_BASE/debian-rootfs-amd64.tar.xz"
    const val DEBIAN_ARM64 = "$DEBIAN_ROOTFS_BASE/debian-rootfs-arm64.tar.xz"
}
