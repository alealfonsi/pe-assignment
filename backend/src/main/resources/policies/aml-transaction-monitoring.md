# AML Transaction Monitoring Policy

## Purpose and scope

This policy defines how the bank monitors customer transactions for money-laundering and
terrorist-financing risk. It applies to all activity types processed on the platform: card
activity, payment activity (ACH, WIRE, SWIFT, SEPA, P2P) and cryptocurrency activity. Customer
care operators must consult this policy when reviewing flagged customer activity and when
interpreting automated risk signals.

## Risk-based approach

Monitoring follows a risk-based approach. Every automated rule contributes a weighted score to
a transaction, and the aggregate picture across a customer's recent activity determines the
review priority:

- Aggregate rule score 0–19 over the review window: routine, no action required.
- Aggregate rule score 20–49: heightened attention; operator should document a short rationale.
- Aggregate rule score 50–89: enhanced due diligence (EDD) review is required within 5 business days.
- Aggregate rule score 90 and above, or any single critical signal (e.g. mixer exposure,
  blacklisted jurisdiction): immediate escalation to the Financial Crime team.

## High-value cross-border payments

Outbound WIRE or SWIFT payments of 10,000 EUR (or currency equivalent) and above to a country
different from the customer's country of residence must carry a documented economic purpose.
Repeated high-value cross-border payments to the same beneficiary within a 30-day window
without an established business relationship are a red flag and require source-of-funds
verification.

## Structuring (smurfing)

Structuring is the deliberate splitting of transfers to stay below the 10,000 reporting
threshold. Indicative pattern: three or more transfers each between 9,000 and 9,999 within any
72-hour window, particularly to the same or related beneficiaries. When a structuring pattern
is detected the operator must NOT inform the customer that a report may be filed (tipping-off
prohibition) and must escalate to the Financial Crime team for a suspicious activity report
(SAR) evaluation.

## Velocity and dormancy anomalies

A sudden spike in activity relative to the customer's own baseline is a monitored behaviour:
a 7-day transaction volume exceeding five times the trailing 90-day weekly average triggers a
velocity signal. A previously dormant account (no activity for 60+ days) that suddenly sends
multiple transfers within days is a classic mule-account and account-takeover indicator and
should be reviewed together with recent login and device history.

## Round-amount patterns

Legitimate retail activity rarely produces repeated large round amounts. Three or more
transfers of exactly round thousands (5,000 and above) within 7 days, especially P2P or to
newly added beneficiaries, indicate possible layering. Combine this signal with beneficiary
concentration: repeated round-amount transfers to the same small set of accounts strengthen
the suspicion.
