package minecraftbyexample.mbe03_block_variants;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 24/12/2014
 *
 * BlockVariants uses a model which
 * - doesn't occupy the entire 1x1x1m space,
 * - is made up of two pieces,
 * - uses a CUTOUT texture (with seethrough holes)
 * - has variants (can face in four directions, and can be four different colours)
 * We can walk over it without colliding.
 * For background information on blocks see here http://greyminecraftcoder.blogspot.com.au/2014/12/blocks-18.html
 */
public class BlockVariants extends Block
{
  public BlockVariants()
  {
    super(Material.rock);
    this.setCreativeTab(CreativeTabs.tabBlock);   // the block will appear on the Blocks tab in creative
  }

  // the block will render in the CUTOUT layer.  See http://greyminecraftcoder.blogspot.co.at/2014/12/block-rendering-18.html for more information.
  @SideOnly(Side.CLIENT)
  public EnumWorldBlockLayer getBlockLayer()
  {
    return EnumWorldBlockLayer.CUTOUT;
  }

  // used by the renderer to control lighting and visibility of other blocks.
  // set to false because this block doesn't fill the entire 1x1x1 space
  @Override
  public boolean isOpaqueCube() {
    return false;
  }

  // used by the renderer to control lighting and visibility of other blocks, also by
  // (eg) wall or fence to control whether the fence joins itself to this block
  // set to false because this block doesn't fill the entire 1x1x1 space
  @Override
  public boolean isFullCube() {
    return false;
  }

  // render using a BakedModel (mbe01_block_simple.json --> mbe01_block_simple_model.json)
  // not strictly required because the default (super method) is 3.
  @Override
  public int getRenderType() {
    return 3;
  }

  // by returning a null collision bounding box we stop the player from colliding with it
  @Override
  public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state)
  {
    return null;
  }

  // Our block has two properties:
  // 1) PROPERTYFACING for which way the sign points (east, west, north, south).  EnumFacing is as standard used by vanilla for a number of blocks.
  // 2) PROPERTYFACING for the sign's colour.  ColoursEnum is a custom class (see below)
  public static final PropertyDirection PROPERTYFACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
  public static final PropertyEnum PROPERTYCOLOUR = PropertyEnum.create("colour", EnumColour.class);

  // this function returns the correct item type corresponding to the colour of our block;
  // i.e. when a sign is broken, it will drop the correct item.
  @Override
  public int damageDropped(IBlockState state)
  {
    EnumColour enumColour = (EnumColour)state.getValue(PROPERTYCOLOUR);
    return enumColour.getMetadata();
  }

  // create a list of the subBlocks available for this block, i.e. one for each colour
  //  - used to populate items for the creative inventory
  // - the "metadata" value of the block is set to the colours metadata
  @Override
  @SideOnly(Side.CLIENT)
  public void getSubBlocks(Item itemIn, CreativeTabs tab, List list)
  {
    EnumColour[] allColours = EnumColour.values();
    for (EnumColour colour : allColours) {
      list.add(new ItemStack(itemIn, 1, colour.getMetadata()));
    }
  }

  // getStateFromMeta, getMetaFromState are used to interconvert between the block's property values and
  //   the stored metadata (which must be an integer in the range 0 - 15 inclusive)
  // The property is encode as:
  // - lower two bits = facing direction (i.e. 0, 1, 2, 3)
  // - upper two bits = colour (i.e. 0, 4, 8, 12)
  @Override
  public IBlockState getStateFromMeta(int meta)
  {
    EnumFacing facing = EnumFacing.getHorizontal(meta);
    int colourbits = (meta & 0x0c) >> 2;
    EnumColour colour = EnumColour.byMetadata(colourbits);
    return this.getDefaultState().withProperty(PROPERTYCOLOUR, colour).withProperty(PROPERTYFACING, facing);
  }

  @Override
  public int getMetaFromState(IBlockState state)
  {
    EnumFacing facing = (EnumFacing)state.getValue(PROPERTYFACING);
    EnumColour colour = (EnumColour)state.getValue(PROPERTYCOLOUR);

    int facingbits = facing.getHorizontalIndex();
    int colourbits = colour.getMetadata() << 2;
    return facingbits | colourbits;
  }

  // this method isn't required if your properties only depend on the stored metadata.
  // it is required if:
  // 1) you are making a multiblock which stores information in other blocks eg BlockBed, BlockDoor
  // 2) your block's state depends on other neighbours (eg BlockFence)
  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
  {
    return state;
  }

  // necessary to define which properties your blocks use
  // will also affect the variants listed in the blockstates model file
  @Override
  protected BlockState createBlockState()
  {
    return new BlockState(this, new IProperty[] {PROPERTYFACING, PROPERTYCOLOUR});
  }

  // when the block is placed, set the appropriate facing direction based on which way the player is looking
  // the colour of block is contained in meta, it corresponds to the values we used for getSubBlocks
  @Override
  public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing blockFaceClickedOn, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  {
    EnumColour colour = EnumColour.byMetadata(meta);
    // find the quadrant the player is facing, add 0.5 for half a quadrant to get proper roundoff to east/west/north/south, then
    //   extract the lower 2 bits (0-3) to account for possible wrap around of the angle
    int playerFacingDirection = (placer == null) ? 0 : MathHelper.floor_double((placer.rotationYaw / 90.0F) + 0.5D) & 3;
    EnumFacing enumfacing = EnumFacing.getHorizontal(playerFacingDirection);

    return this.getDefaultState().withProperty(PROPERTYFACING, enumfacing).withProperty(PROPERTYCOLOUR, colour);
  }

  // create a new enum for our four colours, with some supporting methods to convert to & from metadata, and to get
  //  human-readable names.
  public static enum EnumColour implements IStringSerializable
  {
    BLUE(0, "blue"),
    RED(1, "red"),
    GREEN(2, "green"),
    YELLOW(3, "yellow");

    public int getMetadata()
    {
      return this.meta;
    }

    @Override
    public String toString()
    {
      return this.name;
    }

    public static EnumColour byMetadata(int meta)
    {
      if (meta < 0 || meta >= META_LOOKUP.length)
      {
        meta = 0;
      }

      return META_LOOKUP[meta];
    }

    public String getName()
    {
      return this.name;
    }

    private final int meta;
    private final String name;
    private static final EnumColour[] META_LOOKUP = new EnumColour[values().length];

    private EnumColour(int i_meta, String i_name)
    {
      this.meta = i_meta;
      this.name = i_name;
    }

    static
    {
      for (EnumColour colour : values()) {
        META_LOOKUP[colour.getMetadata()] = colour;
      }
    }
  }
}
