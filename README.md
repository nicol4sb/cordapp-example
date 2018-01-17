Fork of the Corda example for the CS hackaton

![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Non disclosure agreements smart contracts with Corda

Prototype providing a workflow tool for parties wanting to request, edit, and sign NDAs in a trackable fashion


1) Each party has a ledger and can submit a proposal to other parties, of type unilateral or bilateral (NDAEmissionToReviewFlow)
2) a) The recipients can edit the proposition and return the draft to the emitter (NDAReviewAndResponseFlow)
2) b) They can accept it and sign it (NDASignatureFlow)


states : 
- reviewContent (can only be done by recipients)
- reviewComments (can only be done by emitter)
- signed (by all parties)
