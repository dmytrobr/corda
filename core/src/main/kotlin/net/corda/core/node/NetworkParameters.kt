package net.corda.core.node

import net.corda.core.KeepForDJVM
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.DeprecatedConstructorForDeserialization
import net.corda.core.utilities.days
import java.time.Duration
import java.time.Instant

/**
 * Network parameters are a set of values that every node participating in the zone needs to agree on and use to
 * correctly interoperate with each other.
 * @property minimumPlatformVersion Minimum version of Corda platform that is required for nodes in the network.
 * @property notaries List of well known and trusted notary identities with information on validation type.
 * @property maxMessageSize This is currently ignored. However, it will be wired up in a future release.
 * @property maxTransactionSize Maximum permitted transaction size in bytes.
 * @property modifiedTime Last modification time of network parameters set.
 * @property epoch Version number of the network parameters. Starting from 1, this will always increment on each new set
 * of parameters.
 * @property whitelistedContractImplementations List of whitelisted jars containing contract code for each contract class.
 *  This will be used by [net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint]. [You can learn more about contract constraints here](https://docs.corda.net/api-contract-constraints.html).
 * @property eventHorizon Time after which nodes will be removed from the network map if they have not been seen
 * during this period
 */
@KeepForDJVM
@CordaSerializable
data class NetworkParameters(
        val minimumPlatformVersion: Int,
        val notaries: List<NotaryInfo>,
        val maxMessageSize: Int,
        val maxTransactionSize: Int,
        val modifiedTime: Instant,
        val epoch: Int,
        val whitelistedContractImplementations: Map<String, List<AttachmentId>>,
        val eventHorizon: Duration
) {
    @DeprecatedConstructorForDeserialization(1)
    constructor (minimumPlatformVersion: Int,
                 notaries: List<NotaryInfo>,
                 maxMessageSize: Int,
                 maxTransactionSize: Int,
                 modifiedTime: Instant,
                 epoch: Int,
                 whitelistedContractImplementations: Map<String, List<AttachmentId>>
    ) : this(minimumPlatformVersion,
            notaries,
            maxMessageSize,
            maxTransactionSize,
            modifiedTime,
            epoch,
            whitelistedContractImplementations,
            Int.MAX_VALUE.days
    )

    init {
        require(minimumPlatformVersion > 0) { "minimumPlatformVersion must be at least 1" }
        require(notaries.distinctBy { it.identity } == notaries) { "Duplicate notary identities" }
        require(epoch > 0) { "epoch must be at least 1" }
        require(maxMessageSize > 0) { "maxMessageSize must be at least 1" }
        require(maxTransactionSize > 0) { "maxTransactionSize must be at least 1" }
        require(!eventHorizon.isNegative) { "eventHorizon must be positive value" }
    }

    fun copy(minimumPlatformVersion: Int,
             notaries: List<NotaryInfo>,
             maxMessageSize: Int,
             maxTransactionSize: Int,
             modifiedTime: Instant,
             epoch: Int,
             whitelistedContractImplementations: Map<String, List<AttachmentId>>
    ): NetworkParameters {
        return copy(minimumPlatformVersion = minimumPlatformVersion,
                notaries = notaries,
                maxMessageSize = maxMessageSize,
                maxTransactionSize = maxTransactionSize,
                modifiedTime = modifiedTime,
                epoch = epoch,
                whitelistedContractImplementations = whitelistedContractImplementations,
                eventHorizon = eventHorizon)
    }

    override fun toString(): String {
        return """NetworkParameters {
  minimumPlatformVersion=$minimumPlatformVersion
  notaries=$notaries
  maxMessageSize=$maxMessageSize
  maxTransactionSize=$maxTransactionSize
  whitelistedContractImplementations {
    ${whitelistedContractImplementations.entries.joinToString("\n    ")}
  }
  eventHorizon=$eventHorizon
  modifiedTime=$modifiedTime
  epoch=$epoch
}"""
    }
}

/**
 * Data class storing information about notaries available in the network.
 * @property identity Identity of the notary (note that it can be an identity of the distributed node).
 * @property validating Indicates if the notary is validating.
 */
@KeepForDJVM
@CordaSerializable
data class NotaryInfo(val identity: Party, val validating: Boolean)

/** For use by the [checkVersion] function. */
data class PlatformVersionInfo(val minimumPlatformVersion: Int) {
    companion object {
        /**
         * Used for restricting the use of features which require a platform version greater than the current minimum
         * platform version for the network.
         *
         * @param requiredMinimumVersion the minimum platform version which this feature requires.
         */
        fun checkMinimumPlatformVersion(requiredMinimumPlatformVersion: Int) {
            val minimumPlatformVersion = platformVersionInfo.minimumPlatformVersion
            if (requiredMinimumPlatformVersion > minimumPlatformVersion) {
                throw UncheckedVersionException("This feature is disabled until network minimum platform version is " +
                        "increased from $minimumPlatformVersion to $requiredMinimumPlatformVersion.")
            }
        }
    }
}

/**
 * This variable, which is set in [AbstractNode] during node startup, is set to the platform version of the node. It is
 * made available as a thread local variable so that it is easily accessible.
 *
 * WARNING: When using the MockNetwork in single threaded mode (threadPerNode = false) each node must run with the same
 * platform version. To run MockNodes with different platform versions for testing, the MockNetwork must be run in
 * multi-threaded mode (threadPerNode = true).
 */
private val _platformVersionInfo: InheritableThreadLocal<PlatformVersionInfo> = InheritableThreadLocal()

var platformVersionInfo: PlatformVersionInfo
    get() = _platformVersionInfo.get()
            ?: error("Platform version info has not yet been set or its not available on this thread.")
    set(platformVersion) = _platformVersionInfo.set(platformVersion)

class UncheckedVersionException(message: String) : RuntimeException(message)