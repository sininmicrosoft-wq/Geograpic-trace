package com.example.blockchain

object OpContractTemplate {

    const val TOKEN_SOLIDITY_CODE: String = """// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title GeoTraceToken (${'$'}GEOT)
 * @dev ERC-20 Reward Token deployed on Optimism (OP Mainnet / OP Sepolia).
 * Distributed as proof-of-movement rewards for outdoor GPS location tracing.
 */
contract GeoTraceToken is ERC20, ERC20Burnable, Ownable {

    uint256 public constant MAX_SUPPLY = 100_000_000 * 10**18; // 100,000,000 ${'$'}GEOT Cap
    mapping(address => bool) public authorizedMinters;

    event MinterUpdated(address indexed minter, bool status);
    event MovementRewardClaimed(
        address indexed explorer,
        uint256 amount,
        bytes32 indexed traceHash,
        uint256 distanceMeters
    );

    modifier onlyMinter() {
        require(authorizedMinters[msg.sender] || msg.sender == owner(), "Caller not minter");
        _;
    }

    constructor(address initialOwner) ERC20("GeoTrace Token", "GEOT") Ownable(initialOwner) {
        authorizedMinters[initialOwner] = true;
        // Initial 1,000,000 ${'$'}GEOT minted for community rewards & liquidity pool
        _mint(initialOwner, 1_000_000 * 10**decimals());
    }

    function setMinter(address minter, bool status) external onlyOwner {
        authorizedMinters[minter] = status;
        emit MinterUpdated(minter, status);
    }

    /**
     * @dev Mint reward tokens for validated GPS route tracing telemetry
     */
    function mintTraceReward(
        address recipient,
        uint256 amount,
        bytes32 traceHash,
        uint256 distanceMeters
    ) external onlyMinter {
        require(totalSupply() + amount <= MAX_SUPPLY, "Max supply exceeded");
        _mint(recipient, amount);
        emit MovementRewardClaimed(recipient, amount, traceHash, distanceMeters);
    }

    /**
     * @dev Batch reward distribution for quests and trail exploration milestones
     */
    function batchReward(address[] calldata recipients, uint256[] calldata amounts) external onlyMinter {
        require(recipients.length == amounts.length, "Mismatched arrays");
        for (uint256 i = 0; i < recipients.length; i++) {
            require(totalSupply() + amounts[i] <= MAX_SUPPLY, "Max supply exceeded");
            _mint(recipients[i], amounts[i]);
        }
    }
}"""

    const val STAKING_SOLIDITY_CODE: String = """// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/**
 * @title GeoTraceStakingVault
 * @dev Staking contract for ${'$'}GEOT on Optimism.
 * Provides passive APY yields and activates GPS movement boost multipliers.
 */
contract GeoTraceStakingVault is Ownable, ReentrancyGuard {
    IERC20 public immutable geotToken;

    struct StakePosition {
        uint256 amount;
        uint256 startTime;
        uint256 lockDuration;
        uint256 apyBasisPoints; // e.g. 1800 = 18% APY
        uint256 lastClaimTime;
        bool active;
    }

    mapping(address => StakePosition[]) public userStakes;
    mapping(address => uint256) public totalUserStaked;

    event Staked(address indexed user, uint256 amount, uint256 lockDuration, uint256 apy);
    event YieldClaimed(address indexed user, uint256 rewardAmount);
    event Unstaked(address indexed user, uint256 amount);

    constructor(address _geotToken, address initialOwner) Ownable(initialOwner) {
        geotToken = IERC20(_geotToken);
    }

    function stake(uint256 amount, uint256 lockDays, uint256 apyBps) external nonReentrant {
        require(amount > 0, "Cannot stake 0");
        geotToken.transferFrom(msg.sender, address(this), amount);

        userStakes[msg.sender].push(StakePosition({
            amount: amount,
            startTime: block.timestamp,
            lockDuration: lockDays * 1 days,
            apyBasisPoints: apyBps,
            lastClaimTime: block.timestamp,
            active: true
        }));

        totalUserStaked[msg.sender] += amount;
        emit Staked(msg.sender, amount, lockDays, apyBps);
    }

    function claimYield(uint256 stakeIndex) public nonReentrant {
        StakePosition storage pos = userStakes[msg.sender][stakeIndex];
        require(pos.active, "Stake not active");

        uint256 pending = calculatePendingYield(msg.sender, stakeIndex);
        require(pending > 0, "No yield pending");

        pos.lastClaimTime = block.timestamp;
        geotToken.transfer(msg.sender, pending);
        emit YieldClaimed(msg.sender, pending);
    }

    function unstake(uint256 stakeIndex) external nonReentrant {
        StakePosition storage pos = userStakes[msg.sender][stakeIndex];
        require(pos.active, "Stake not active");
        require(block.timestamp >= pos.startTime + pos.lockDuration, "Lock not matured");

        if (calculatePendingYield(msg.sender, stakeIndex) > 0) {
            claimYield(stakeIndex);
        }

        pos.active = false;
        totalUserStaked[msg.sender] -= pos.amount;
        geotToken.transfer(msg.sender, pos.amount);
        emit Unstaked(msg.sender, pos.amount);
    }

    function calculatePendingYield(address user, uint256 stakeIndex) public view returns (uint256) {
        StakePosition memory pos = userStakes[user][stakeIndex];
        if (!pos.active) return 0;
        uint256 elapsed = block.timestamp - pos.lastClaimTime;
        return (pos.amount * pos.apyBasisPoints * elapsed) / (10000 * 365 days);
    }
}"""

    const val SOLIDITY_CODE: String = TOKEN_SOLIDITY_CODE

    const val DEPLOYMENT_GUIDE: String = """
1. Open Remix IDE (https://remix.ethereum.org)
2. Deploy `GeoTraceToken.sol` first on OP Sepolia (Chain ID 11155420) or OP Mainnet (Chain ID 10).
3. Then create `GeoTraceStakingVault.sol` and paste the Staking contract code.
4. Pass the deployed ${'$'}GEOT token address to the Staking Vault constructor.
5. Fund the Staking Vault with ${'$'}GEOT tokens for reward yield distribution.
6. Users can now stake ${'$'}GEOT to earn passive APY yields and activate GPS route trace reward boosts!
"""
}
