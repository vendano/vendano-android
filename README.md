# Vendano Android

### Simple, contact-based ADA for everyone

Vendano is an open-source mobile wallet that lets anyone **send and receive ADA (and native tokens) using a phone number or e-mail address** instead of a 58-character address.

The goal is to make Cardano transactions feel as familiar as texting: no arcane jargon and an interface that guides first-time users in plain language.

> **Why there’s room for Vendano**
> 
> Most Cardano wallets excel at power-user features (staking dashboards, hardware-wallet pairing, multi-asset portfolios), yet still ask newcomers to master new concepts before they can move a single coin. Vendano's mission focuses on **on-ramp simplicity**: features such as verified contacts, one-tap transfers, and automatic on-chain “claim” flows for recipients who haven’t installed the app yet.

### Repository Overview

Vendano Android is the Kotlin-based Android client for the Vendano wallet. This repository contains the Android app codebase for review of architecture, data flows, and security/privacy practices. Info.plist files are not relevant on Android; the repository omits secrets (eg, google-services.json and keystore details) to prevent leaking keys. The code is under active development and is intended to be built and shipped after review.


### Review and contributing

- Reviewers: focus on seed handling, authentication flows, data flows (hashed contacts), network calls, and how secrets are managed.
- See CONTRIBUTING.md for how to propose changes, run checks, and file security concerns.
- If you identify security concerns, please open an issue or a pull request with a clear description and steps to reproduce.

### Licensing

BSD 3-Clause

### Contact

- Support: support@vendano.net
- Website: https://vendano.net
- Policy pages (privacy/terms): https://vendano.net/privacy.html, https://vendano.net/terms.html