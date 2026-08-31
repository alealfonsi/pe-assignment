# Crypto-Asset Activity Policy

## Scope

This policy governs the monitoring of cryptocurrency activity: purchases and sales through
exchanges, and on-chain transfers between wallets. Fields evaluated include blockchain,
source and destination wallet addresses, transaction hash and the involved exchange, if any.

## Approved exchanges (VASP list)

The following virtual asset service providers are registered and approved for customer
on/off-ramp activity: Coinbase, Kraken, Bitstamp, Gemini. Activity through an exchange not
on this list — including Binance pending its registration review — is permitted but
generates an elevated-risk signal and counts toward the customer's aggregate risk score.
Transfers where no exchange is identified (self-hosted or unknown counterparty wallets)
must be treated as unhosted-wallet transfers.

## Mixing and tumbling services

Transfers to known mixing/tumbling services obscure the source of funds and are treated as a
critical signal regardless of amount. The current internal address list of known mixer entry
points includes, among others, the MixCloud Tumbler router address
0xe67641025b565d24a75dc5d56b5b28ce2620d835 on Ethereum. Any transfer to a listed mixer
address — completed, pending or failed — requires same-day escalation to the Financial Crime
team and freezes further crypto withdrawals pending review. A failed or blocked attempt to
send funds to a mixer is itself reportable behaviour and must not be discarded because the
transfer did not settle.

## Rapid outflow patterns

Three or more outgoing on-chain transfers within a 24-hour window is a rapid-outflow
pattern. When it follows a short accumulation phase (funds bought on exchanges over the
preceding days or weeks and then dispersed to multiple unhosted wallets), the pattern is
consistent with layering or an exit scam and warrants HIGH risk classification. Compare the
outflow total against the customer's total accumulated position: near-complete dispersal
(over 80% of holdings moved out) is an aggravating factor.

## Unhosted wallet transfers

Transfers to unhosted (self-custodied) wallets are legal but reduce traceability. For
cumulative unhosted-wallet outflows above 1,000 EUR equivalent in 30 days, the travel-rule
data must be requested from the customer: beneficiary name and wallet ownership
confirmation. Repeated transfers to many distinct fresh unhosted addresses (peel-chain
behaviour) are an additional red flag.

## Off-boarding and communication guidance

Do not discuss internal risk scoring or possible SAR filings with the customer
(tipping-off prohibition). If crypto withdrawals are frozen under this policy, the customer
communication template CRY-14 ("temporary security review") must be used verbatim.
