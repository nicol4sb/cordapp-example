package com.example.api;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.flow.ExampleFlow;
import com.example.flow.NDACreationFlow;
import com.example.flow.NDAEditionFlow;
import com.example.state.IOUState;
import com.example.state.NDAContractState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowProgressHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
public class ExampleApi {
	private final CordaRPCOps rpcOps;
	private final CordaX500Name myLegalName;

	private final List<String> serviceNames = ImmutableList.of("Controller", "Network Map Service");

	static private final Logger logger = LoggerFactory.getLogger(ExampleApi.class);

	public ExampleApi(CordaRPCOps rpcOps) {
		this.rpcOps = rpcOps;
		this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
	}

	/**
	 * Returns the node's name.
	 */
	@GET
	@Path("me")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, CordaX500Name> whoami() {
		return ImmutableMap.of("me", myLegalName);
	}

	/**
	 * Returns all parties registered with the [NetworkMapService]. These names can
	 * be used to look up identities using the [IdentityService].
	 */
	@GET
	@Path("peers")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, List<CordaX500Name>> getPeers() {
		List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
		return ImmutableMap.of("peers",
				nodeInfoSnapshot.stream().map(node -> node.getLegalIdentities().get(0).getName())
						.filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
						.collect(toList()));
	}

	/**
	 * Displays all NDA states that exist in the node's vault.
	 */
	@GET
	@Path("ious")
	@Produces(MediaType.APPLICATION_JSON)
	public List<StateAndRef<IOUState>> getIOUs() {
		return rpcOps.vaultQuery(IOUState.class).getStates();
	}

	/**
	 * Initiates a flow to agree an IOU between two parties.
	 *
	 * Once the flow finishes it will have written the IOU to ledger. Both the
	 * lender and the borrower will be able to see it when calling /api/example/ious
	 * on their respective nodes.
	 *
	 * This end-point takes a Party name parameter as part of the path. If the
	 * serving node can't find the other party in its network map cache, it will
	 * return an HTTP bad request.
	 *
	 * The flow is invoked asynchronously. It returns a future when the flow's
	 * call() method returns.
	 */
	@PUT
	@Path("create-iou")
	public Response createIOU(@QueryParam("iouValue") int iouValue, @QueryParam("partyName") CordaX500Name partyName)
			throws InterruptedException, ExecutionException {
		if (iouValue <= 0) {
			return Response.status(BAD_REQUEST).entity("Query parameter 'iouValue' must be non-negative.\n").build();
		}
		if (partyName == null) {
			return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n")
					.build();
		}

		final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);
		if (otherParty == null) {
			return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
		}

		try {
			FlowProgressHandle<SignedTransaction> flowHandle = rpcOps
					.startTrackedFlowDynamic(ExampleFlow.Initiator.class, iouValue, otherParty);
			flowHandle.getProgress().subscribe(evt -> System.out.printf(">> %s\n", evt));

			// The line below blocks and waits for the flow to return.
			final SignedTransaction result = flowHandle.getReturnValue().get();

			final String msg = String.format("Transaction id %s committed to ledger.\n", result.getId());
			return Response.status(CREATED).entity(msg).build();

		} catch (Throwable ex) {
			final String msg = ex.getMessage();
			logger.error(ex.getMessage(), ex);
			return Response.status(BAD_REQUEST).entity(msg).build();
		}
	}

	@PUT
	@Path("create-ndarequest")
	public Response createNDARequest(@QueryParam("ndaRequestText") String ndaRequestText,
			@QueryParam("partyName") CordaX500Name partyName) throws InterruptedException, ExecutionException {
		if (!(ndaRequestText.length() > 20)) {
			return Response.status(BAD_REQUEST)
					.entity("Query parameter 'ndaRequestText' - Put some text in this NDA ! More than 20 chars.\n")
					.build();
		}
		if (partyName == null) {
			return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n")
					.build();
		}

		// Translate the string into a valid Party from the central node/notary service
		final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);
		if (otherParty == null) {
			return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
		}

		try {
			System.out.println("NDARequestText : " + ndaRequestText + " -- otherParty :: " + partyName);
			FlowProgressHandle<SignedTransaction> flowHandle = rpcOps
					.startTrackedFlowDynamic(NDACreationFlow.Initiator.class, ndaRequestText, otherParty);
			flowHandle.getProgress().subscribe(evt -> System.out.printf(">> %s\n", evt));

			// The line below blocks and waits for the flow to return.
			final SignedTransaction result = flowHandle.getReturnValue().get();

			final String msg = String.format("NDA request with id %s committed to ledger.\n", result.getId());
			return Response.status(CREATED).entity(msg).build();

		} catch (Throwable ex) {
			final String msg = ex.getMessage();
			logger.error(ex.getMessage(), ex);
			return Response.status(BAD_REQUEST).entity(msg).build();
		}
	}

	@GET
	@Path("getNdaRequests")
	@Produces(MediaType.APPLICATION_JSON)
	public List<StateAndRef<NDAContractState>> getNdaRequests() {
		return rpcOps.vaultQuery(NDAContractState.class).getStates();
	}

	@PUT
	@Path("review-nda")
	public Response reviewNDA(@QueryParam("ndaRequestText") String ndaRequestText,
			@QueryParam("ndaPreviousStateId") String ndaPreviousStateId)
			throws InterruptedException, ExecutionException {
		if (!(ndaRequestText.length() > 20)) {
			return Response.status(BAD_REQUEST)
					.entity("Query parameter 'ndaRequestText' - Put some text in this NDA ! More than 20 chars.\n")
					.build();
		}
		if (ndaPreviousStateId == null) {
			return Response.status(BAD_REQUEST).entity("Query parameter 'ndaPreviousStateId' missing.\n").build();
		}

		// Translate the previous state id into a the state object
		List<StateAndRef<NDAContractState>> previousStates = rpcOps.vaultQuery(NDAContractState.class).getStates();
		NDAContractState previousNDAContractState = null;

		for (StateAndRef<NDAContractState> stateAndRef : previousStates) {
			if (stateAndRef.getState().getData().getLinearId().toString().equals(ndaPreviousStateId)) {
				previousNDAContractState = stateAndRef.getState().getData();
			}
		}

		try {
			System.out
					.println("NDARequestText : " + ndaRequestText + " -- ndaPreviousStateId :: " + ndaPreviousStateId);
			FlowProgressHandle<SignedTransaction> flowHandle = rpcOps
					.startTrackedFlowDynamic(NDAEditionFlow.Initiator.class, ndaRequestText, previousNDAContractState);
			flowHandle.getProgress().subscribe(evt -> System.out.printf(">> %s\n", evt));

			// The line below blocks and waits for the flow to return.
			final SignedTransaction result = flowHandle.getReturnValue().get();

			final String msg = String.format("NDA request with id %s committed to ledger.\n", result.getId());
			return Response.status(CREATED).entity(msg).build();

		} catch (Throwable ex) {
			final String msg = ex.getMessage();
			logger.error(ex.getMessage(), ex);
			return Response.status(BAD_REQUEST).entity(msg).build();
		}
	}

}