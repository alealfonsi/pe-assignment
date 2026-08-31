# Structuring and Layering Red Flags

## What structuring looks like

Structuring (smurfing) splits money movements into amounts below reporting thresholds. The
canonical presentation on this platform is a series of transfers between 9,000 and 9,999
executed close together in time. Variants operators should recognise:

- Same-day splits across multiple beneficiaries that reconcile to a round total.
- Alternating channels: part by wire, part by P2P, part converted to crypto, keeping each
  channel below its individual threshold.
- Third-party involvement: transfers routed through a family member's or associate's
  account to break the direct link.

## Layering indicators

Layering hides the origin of funds through movement chains. Indicators observable in this
platform's data include: funds arriving and leaving within short windows (pass-through
behaviour), conversion into crypto followed by rapid on-chain dispersal, round-amount
transfer chains, and corridor-hopping across jurisdictions with no economic rationale.

## Interaction with other signals

Structuring rarely appears alone. Score the case higher when structuring co-occurs with:
a greylist or blacklist jurisdiction corridor (see High-Risk Jurisdictions Policy),
a velocity spike against the customer's baseline, or beneficiary accounts opened recently.
A structuring pattern immediately following large inbound credits that are inconsistent
with the customer's profile (salary, declared business) is a strong SAR candidate.

## What operators must and must not do

Must: preserve the full transaction context, document the pattern with transaction
references, and escalate to the Financial Crime team for SAR evaluation within 24 hours of
detection. Must not: contact the customer about the suspicion, delay or block transactions
in a way that reveals the review (unless instructed by Financial Crime), or annotate
customer-visible channels with suspicion details. Tipping-off is a regulatory offence.
