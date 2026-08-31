# High-Risk Jurisdictions Policy

## Purpose

This policy lists the jurisdictions subject to restrictions or enhanced monitoring for
payment activity, and the handling rules for each tier. Beneficiary bank country
(receiver_bank_country) is the primary field evaluated for payment activity.

## Tier 1 — Prohibited jurisdictions (blacklist)

Payments to or from beneficiary banks in the following countries are prohibited and must be
blocked and escalated immediately: IR (Iran), KP (North Korea), SY (Syria), CU (Cuba).
Any completed transaction involving a Tier 1 jurisdiction is a critical incident: notify the
Sanctions desk the same business day.

## Tier 2 — Enhanced monitoring jurisdictions (greylist)

Payments to beneficiary banks in the following countries are permitted but subject to
enhanced due diligence: TR (Türkiye), AE (United Arab Emirates), GE (Georgia), HK (Hong
Kong), PA (Panama), MM (Myanmar), AF (Afghanistan). For any single payment of 10,000 EUR or
more, or cumulative payments of 25,000 EUR or more in 30 days to Tier 2 jurisdictions, the
operator must verify documented economic purpose and source of funds. Repeated corridor use
(3+ payments to the same Tier 2 country in 90 days) without a documented business
relationship requires a Financial Crime referral.

## Corridor risk notes

- AE and HK corridors are frequently used for trade-based money laundering; request
  invoices or contracts supporting the transfers.
- GE and TR corridors have elevated exposure to sanctions-evasion networks re-routing funds
  for restricted counterparties; check beneficiary names against the sanctions screening
  results and look for third-party payers.
- P2P transfers have no beneficiary bank country validation beyond IBAN prefix; treat the
  IBAN country code as the effective jurisdiction.

## Customer residence vs. transaction corridor

A customer whose residence country differs from a repeated payment corridor without an
obvious link (nationality, registered business activity, declared employer) is an anomaly.
Document the explanation given by the customer; unexplained corridor use combined with
high-value or structured payments raises the case to HIGH risk at minimum.
