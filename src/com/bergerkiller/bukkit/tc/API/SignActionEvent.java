package com.bergerkiller.bukkit.tc.API;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import com.bergerkiller.bukkit.tc.Destinations;
import com.bergerkiller.bukkit.tc.MinecartGroup;
import com.bergerkiller.bukkit.tc.MinecartMember;
import com.bergerkiller.bukkit.tc.signactions.SignActionMode;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.BlockUtil;
import com.bergerkiller.bukkit.tc.utils.FaceUtil;

public class SignActionEvent extends Event implements Cancellable {
	private static final long serialVersionUID = 2L;

	public SignActionEvent(Block signblock, MinecartMember member) {
		this(signblock);
		this.member = member;
		this.memberchecked = true;
	}
	public SignActionEvent(Block signblock, MinecartGroup group) {
		this(signblock);
		this.group = group;
		this.memberchecked = true;
	}
	public SignActionEvent(SignActionType actionType, Block signblock, MinecartMember member) {
		this(actionType, signblock);
		this.member = member;
		this.memberchecked = true;
	}
	public SignActionEvent(SignActionType actionType, Block signblock, MinecartGroup group) {
		this(actionType, signblock);
		this.group = group;
		this.memberchecked = true;
	}	
	public SignActionEvent(SignActionType actionType, Block signblock) {
		this(signblock);
		this.actionType = actionType;
	}
	public SignActionEvent(final Block signblock, final Block railsblock) {
		super("SignActionEvent");
		this.signblock = signblock;
		this.sign = BlockUtil.getSign(signblock);
		this.mode = SignActionMode.fromSign(this.sign);
		this.railsblock = railsblock;
	}
	public SignActionEvent(final Block signblock) {
		super("SignActionEvent");
		this.signblock = signblock;
		this.sign = BlockUtil.getSign(signblock);
		this.mode = SignActionMode.fromSign(this.sign);
		//try to find out where the rails block is located
		Block above = this.signblock.getRelative(0, 2, 0);
		if (BlockUtil.isRails(above)) {
			this.railsblock = above;
		} else {
			//rail located above the attached face?
			BlockFace face = BlockUtil.getAttachedFace(this.signblock);
			above = this.signblock.getRelative(face.getModX(), 1, face.getModZ());
			if (BlockUtil.isRails(above)) {
				this.railsblock = above;
			} else {
				this.railsblock = null;
			}
		}
	}

	private final Block signblock;
	private final Block railsblock;
	private final SignActionMode mode;
	private SignActionType actionType;
	private BlockFace facing = null;
	private final Sign sign;
	private BlockFace raildirection = null;
	private MinecartMember member = null;
	private MinecartGroup group = null;
	private boolean memberchecked = false;
	private boolean cancelled = false;
	
	public void setLevers(boolean down) {
		BlockUtil.setLeversAroundBlock(this.getAttachedBlock(), down);
	}
	public void setRails(BlockFace to) {
		BlockUtil.setRails(this.getRails(), this.getFacing(), to);
	}
	public void setRailsRelative(BlockFace direction) {
	  BlockFace main = this.getFacing().getOppositeFace();
		setRails(FaceUtil.offset(main, direction));
	}
	
	/**
	 * Sets rail of current event in given direction, coming from direction the minecart is coming from.
	 * This will go straight if trying to go into the direction the cart is coming from.
	 * This function requires a MinecartMember to work!
	 * @param to Absolute direction to go to.
	 */
	public void setRailsFromCart(BlockFace to) {
		BlockUtil.setRails(this.getRails(), this.getMember().getDirection().getOppositeFace(), to);
		if (this.getMember().getDirection().getOppositeFace() == to){
			this.getGroup().stop();
			//TODO: Safe force factor removal
			this.getGroup().clearActions();
			this.getMember().addActionLaunch(to, 1, this.getMember().getForce());
		}
	}
	public void setRailsRelativeFromCart(BlockFace direction) {
		setRailsFromCart(getRelativeFromCart(direction));
	}
	public BlockFace getRelativeFromCart(BlockFace to) {
		return FaceUtil.offset(this.getMember().getDirection(), to);
	}
	public void setRails(boolean left, boolean right) {
		if (right) {
			setRailsRight();
		} else if (left) {
			setRailsLeft();
		} else {
			setRailsForward();
		}
	}
	public void setRailsLeft() {
		//is a track present at this direction?
		BlockFace main = this.getRelativeFromCart(BlockFace.WEST);
		if (!BlockUtil.isRails(this.getRails().getRelative(main))) {
			main = this.getRelativeFromCart(BlockFace.NORTH);
		}
		//Set it
		this.setRailsFromCart(main);
	}
	public void setRailsRight() {
		//is a track present at this direction?
		BlockFace main = this.getRelativeFromCart(BlockFace.EAST);
		if (!BlockUtil.isRails(this.getRails().getRelative(main))) {
			main = this.getRelativeFromCart(BlockFace.NORTH);
		}
		//Set it
		this.setRailsFromCart(main);
	}
	public void setRailsForward() {
		//is a track present at this direction?
		BlockFace main = this.getRelativeFromCart(BlockFace.NORTH);
		if (!BlockUtil.isRails(this.getRails().getRelative(main))) {
			main = this.getRelativeFromCart(BlockFace.EAST);
			if (!BlockUtil.isRails(this.getRails().getRelative(main))) {
				main = this.getRelativeFromCart(BlockFace.WEST);
			}
		}
		//Set it
		this.setRailsFromCart(main);
	}

	/**
	 * Finds the direction to go in to reach destination from here.
	 * Designed to be used by self-routing tag signs.
	 * If the destination is not known or reachable, goes NORTH.
	 * @param destination The wanted destination to reach.
	 * @return The direction to go in to reach the wanted destination.
	 */
	public BlockFace getDestDir(String destination){
	  return Destinations.getDir(destination, this.getLocation().add(0, 2, 0));
	}
	
	public SignActionType getAction() {
		return this.actionType;
	}
	public boolean isAction(SignActionType... types) {
		for (SignActionType type : types) {
			if (this.actionType == type) return true;
		}
		return false;
	}
	public void setAction(SignActionType type) {
		this.actionType = type;
	}
	
	public boolean isPowered(BlockFace from) {
		return this.getBlock().getRelative(from).isBlockIndirectlyPowered();
	}
	public boolean isPowered() {
		return this.getBlock().isBlockIndirectlyPowered() ||
				isPowered(BlockFace.NORTH) ||
				isPowered(BlockFace.EAST) ||
				isPowered(BlockFace.SOUTH) ||
				isPowered(BlockFace.WEST);
	}
	public boolean isPoweredFacing() {
		return this.actionType == SignActionType.REDSTONE_ON || (this.isFacing() && this.isPowered());
	}
	public Block getBlock() {
		return this.signblock;
	}
	public Block getAttachedBlock() {
		return BlockUtil.getAttachedBlock(this.signblock);
	}
	public Block getRails() {
		return this.railsblock;
	}
	public boolean hasRails() {
		return this.railsblock != null;
	}
	public BlockFace getRailDirection() {
		if (this.raildirection == null) {
			this.raildirection = BlockUtil.getRails(this.railsblock).getDirection();
		}
		return this.raildirection;
	}
	public Location getRailLocation() {
		return this.getRails().getLocation().add(0.5, 0, 0.5);
	}
	public Location getLocation() {
		return this.signblock.getLocation();
	}
	public BlockFace getFacing() {
		if (this.facing == null) {
			this.facing = BlockUtil.getFacing(this.getBlock());
		}
		return this.facing;
	}
	public boolean isFacing() {
		if (getMember() == null) return false;
		if (!getMember().isMoving()) return false;
		return getMember().getDirection() != getFacing();
	}
	public Sign getSign() {
		return this.sign;
	}
	public MinecartMember getMember() {
		if (!this.memberchecked) {
			this.member = MinecartMember.getAt(getRailLocation(), this.group);
			this.memberchecked = true;
		}
		if (this.member == null && this.group != null && this.group.size() > 0) {
			if (this.actionType == SignActionType.GROUP_LEAVE) {
				this.member = this.group.tail();
			} else {
				this.member = this.group.head();
			}
		}
		return this.member;
	}
	public boolean hasMember() {
		return this.getMember() != null;
	}
	public MinecartGroup getGroup() {
		if (this.group != null) return this.group;
		MinecartMember mm = this.getMember();
		if (mm == null) return null;
		return mm.getGroup();
	}
	public boolean hasGroup() {
		return this.getGroup() != null;
	}
	public String getLine(int index) {
		return this.sign.getLine(index);
	}
	public String[] getLines() {
		return this.sign.getLines();
	}
	public void setLine(int index, String line) {
		this.sign.setLine(index, line);
		this.sign.update(true);
	}
	public SignActionMode getMode() {
		return this.mode;
	}
	public boolean isCartSign() {
		return this.mode == SignActionMode.CART;
	}
	public boolean isTrainSign() {
		return this.mode == SignActionMode.TRAIN;
	}
	public boolean isType(String signtype) {
		return this.getLine(1).toLowerCase().startsWith(signtype);
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	public void setCancelled(boolean arg0) {
		this.cancelled = arg0;	
	}
}