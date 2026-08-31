# Card Fraud Detection Guidance

## Scope

This guidance covers monitoring of card activity: point-of-sale and card-not-present (CNP)
transactions, declines, and merchant category risk. Fields evaluated include masked PAN,
merchant name, MCC code, card-present flag, authorization code and decline reason.

## Decline bursts and card testing

Three or more declined authorizations within 60 minutes indicate probable card testing:
fraudsters validate stolen card data with small purchases before attempting larger ones. A
typical testing sequence starts with a very small amount (often under 5) at a low-friction
online merchant, followed by escalating amounts. Decline reasons citing issuer fraud rules
or "card blocked" during such a burst strongly suggest the card data is compromised. When a
testing pattern is followed within 48 hours by successful high-value card-not-present
purchases, treat the successful purchases as presumptively fraudulent: contact the
cardholder through a verified channel, do not rely on callback numbers provided in recent
contact-detail changes, and initiate the reissue-and-dispute flow (template CARD-7).

## Card-not-present risk

CNP transactions of 2,000 or more are elevated-risk. Aggravating factors: night-time
timestamps (00:00–05:00 local), electronics or easily resellable goods merchants, first
purchase at that merchant, and shipping/contact details changed within the previous 14 days.
A REVERSED status on a high-value CNP purchase usually reflects a cardholder dispute; a
dispute following a decline burst is corroborating evidence of fraud rather than friendly
fraud.

## High-risk merchant categories

The following MCC codes are classified high-risk and generate monitoring signals:
7995 (gambling and betting), 6051 (quasi-cash: crypto and money orders), 5993 (cigar
stores), 4829 (wire transfer money orders). Repeated gambling activity (3+ transactions in
30 days) combined with escalating amounts is both a fraud and a responsible-gambling
concern; note it in the customer file. Quasi-cash MCC activity is functionally a cash
withdrawal and inherits the cash-handling thresholds from the AML Transaction Monitoring
Policy.

## Operator checklist for suspected card fraud

1. Verify whether the cardholder recognises the earliest transaction of the suspicious
   sequence, not the largest.
2. Check card-present vs CNP mix: genuine cardholder POS activity continuing in parallel
   with suspicious CNP activity suggests data compromise rather than lost/stolen card.
3. Block and reissue on confirmed compromise; file the dispute within the scheme deadline.
4. Record the outcome with reason codes so downstream rule tuning stays accurate.
