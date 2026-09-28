{
  description = "Specgetty on Droid: unofficial Android reader for OpenSpec projects in git repositories";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      pkgsFor = system: import nixpkgs {
        inherit system;
        config = {
          allowUnfree = true;
          android_sdk.accept_license = true;
        };
      };

      forAllSystems = f: nixpkgs.lib.genAttrs systems (system: f (pkgsFor system));

      buildToolsVersion = "37.0.0";

      sdkArgs = {
        cmdLineToolsVersion = "22.0";
        platformToolsVersion = "37.0.1";
        buildToolsVersions = [ buildToolsVersion ];
        platformVersions = [ "37.0" "36" "26" ];
        includeSources = false;
        includeNDK = false;
      };

      androidFor = pkgs: pkgs.androidenv.composeAndroidPackages (sdkArgs // {
        includeEmulator = false;
        includeSystemImages = false;
      });

      androidWithEmulatorFor = pkgs: pkgs.androidenv.composeAndroidPackages (sdkArgs // {
        includeEmulator = true;
        includeSystemImages = true;
        systemImageTypes = [ "default" ];
        abiVersions = [ "x86_64" ];
      });
    in
    {
      devShells = forAllSystems (pkgs:
        let
          sdk = (androidFor pkgs).androidsdk;
          sdkRoot = "${sdk}/libexec/android-sdk";
        in
        {
          default = pkgs.mkShell {
            packages = [
              pkgs.jdk17
              pkgs.gradle
              pkgs.kotlin
              sdk
              pkgs.git
              pkgs.jq
              pkgs.shellcheck
            ];

            JAVA_HOME = "${pkgs.jdk17}";
            ANDROID_HOME = sdkRoot;
            ANDROID_SDK_ROOT = sdkRoot;

            shellHook = ''
              export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_SDK_ROOT/build-tools/${buildToolsVersion}/aapt2"
              echo "specgetty-mobile dev shell: jdk $(javac -version 2>&1), sdk at $ANDROID_SDK_ROOT"
            '';
          };

          emulator =
            let
              emuSdk = (androidWithEmulatorFor pkgs).androidsdk;
              emuRoot = "${emuSdk}/libexec/android-sdk";
            in
            pkgs.mkShell {
              packages = [
                pkgs.jdk17
                emuSdk
                pkgs.git
                pkgs.jq
              ];

              JAVA_HOME = "${pkgs.jdk17}";
              ANDROID_HOME = emuRoot;
              ANDROID_SDK_ROOT = emuRoot;

              shellHook = ''
                export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_SDK_ROOT/build-tools/${buildToolsVersion}/aapt2"
                export ANDROID_AVD_HOME="''${ANDROID_AVD_HOME:-$PWD/.avd}"
                echo "specgetty-mobile emulator shell: sdk at $ANDROID_SDK_ROOT, avds in $ANDROID_AVD_HOME"
              '';
            };
        });

      checks = forAllSystems (pkgs: {
        shell-scripts = pkgs.runCommand "check-shell-scripts"
          {
            nativeBuildInputs = [ pkgs.shellcheck ];
            src = ./scripts;
          } ''
          shellcheck "$src"/*.sh
          touch "$out"
        '';

        nix-format = pkgs.runCommand "check-nix-format"
          {
            nativeBuildInputs = [ pkgs.nixpkgs-fmt ];
            src = ./flake.nix;
          } ''
          nixpkgs-fmt --check "$src"
          touch "$out"
        '';
      });

      formatter = forAllSystems (pkgs: pkgs.nixpkgs-fmt);
    };
}
