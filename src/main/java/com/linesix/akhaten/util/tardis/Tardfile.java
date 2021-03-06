package com.linesix.akhaten.util.tardis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.linesix.akhaten.common.Reference;
import com.linesix.akhaten.util.FileUtil;

import com.sun.org.apache.xml.internal.security.signature.ReferenceNotInitializedException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.lwjgl.Sys;
import scala.Int;

public class Tardfile {

    /* Tardfile creation and update class
     *
     * This class is used to create and update "tardFiles"
     * Information on tardFiles can be found under the github Akhaten wiki!
     *
     * Author: Felix Eckert (TheBertrahmPlays / Angry German)
     *
     * TODO: 
     *
     */

    /**
     * Generates a simple Tardfile using PrintWriter.
     *
     * @param worldIn World that the player is currently located in
     * @param pos The position the TARDIS was placed at
     * @param placer The player that placed the TARDIS
     * @param path The Path to the current SaveRootDirectory
     *
     * @throws FileNotFoundException if the json file / the "tardises" directory could not be found
     *
     * @author Felix Eckert
     */
    public static JsonObject genTardfile(World worldIn, BlockPos pos, EntityLivingBase placer, File path) throws FileNotFoundException, NullPointerException{

            Reference.logger.info("Generating tardFile for user" + placer.getName() + "...");

            File pathComplete = new File(FileUtil.combine(path, new File("/tardFile_" + placer.getName() + ".json"))); // Create the whole path
            
            int id = path.getAbsoluteFile().list().length + 1; // ID is based on Number of Tardfiles
            int playerDimension = placer.world.provider.getDimension(); // Get the current dimension of the player
            int interiorX = 100*id+25; // interior x
            int interiorZ = 100*id+25; // interior y
            
            String[] tardfilearray = createTardFileArray(placer.getName(), placer.getUniqueID().toString(), id, interiorX, 64, interiorZ, 
            		pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ(), playerDimension, 0,new boolean[] {false, true}, true); // Create the array containing all base information
            
            try {
            	if (!path.exists()) // If the "tardises" directory doesn't exit, create it
            		path.mkdir(); // Create "tardises" directory
                FileUtil.writeFileFromArray(pathComplete, tardfilearray, FileUtil.LineMods.LN_BREAK); // Finally write the Tardfile
            } catch (FileAlreadyExistsException e) { // If a Tardfile for that user already exists
                Reference.logger.info("File for user " + placer.getName() + "already exists!");
                placer.sendMessage(new TextComponentString("§4You already own a TARDIS! To delete it use /delete-tardis!")); // Send message for deleting tardis
                worldIn.destroyBlock(pos, true); // Destroy the Tardis
            }
            
            // Replace every single quote with a double quote
            try {
                if (!replaceChar(pathComplete)) {
                    placer.sendMessage(new TextComponentString("§4An error has occured and the Tardfile could not be written!"));
                    worldIn.destroyBlock(pos, true);
                    return new JsonObject();
                }
            } catch (IOException e) {
                e.printStackTrace();
                placer.sendMessage(new TextComponentString("§4An error has occured and the tardFile could not be written!"));
                return new JsonObject();
            }
            
            // Register the tardis in the TardfileIndex
            try {
				registerTardfile(id, new int[] {pos.getX(), pos.getY(), pos.getZ()},placer.getName(), id);
			} catch (IOException e) {
				placer.sendMessage(new TextComponentString("§4An error has occured whilst trying to register you TARDIS!"));
				worldIn.destroyBlock(pos, true);
				pathComplete.delete();
				e.printStackTrace();
				return new JsonObject();
			}
            
            // Finally congratulate the player, for getting the TARDIS
            placer.sendMessage(new TextComponentString("§2Congratulations for getting your own §6Type 40 TT Capsule."));
            try {
				return FileUtil.parseJSON(pathComplete);
			} catch (IOException e) {
				e.printStackTrace();
				return new JsonObject();
			}
            
    }

    /**
     * Registers a Tardfile to the tardfileIndex, which is located in the tardises directory of the current SaveRootDirectory
     * 
     * @param id The ID of the TARDIS to register
     * @param owner The Owner of the TARDIS to register
     * 
     * @throws IOException 
     * 
     * @author Felix Eckert
     */
    public static void registerTardfile(int id, int[] xyz, String owner, int tardises) throws IOException {
    	File registry = new File(DimensionManager.getCurrentSaveRootDirectory() + "/tardises/tardfileIndex.json"); // Create a new path to the TardfileIndex/Registry
    	JsonObject registryJSON; // Create a variable for the index/registry to be stored in temporarely
    	
    	if (!registry.exists()) {  // If the registry file doesn't exist
    		FileUtil.writeFile(registry, "{\n}"); // Create it
    		registryJSON = FileUtil.parseJSON(registry); // Parse the registry JSON
    		registryJSON.addProperty("registeredTardises", String.valueOf(tardises));
    	} else {
    		registryJSON = FileUtil.parseJSON(registry); // Parse the registry JSON
    	}
		registryJSON.add(String.valueOf(id), new Gson().fromJson("{'owner':'"+owner+"', 'xyz':[{'val':'"+xyz[0]+"'}, {'val':'"+xyz[1]+"'}, {'val':'"+xyz[2]+"'}]}", JsonObject.class)); // Append the info
		registryJSON.remove("registeredTardises");
		registryJSON.addProperty("registeredTardises", String.valueOf(tardises));
		
		registry.delete(); // Delete the old registry file

		FileUtil.writeFile(registry, registryJSON.toString()); // Write the new one
    }
    
    /**
     * Updates a tardfile
     *
     * @param path The path of the tardfile to update
     * @param coords The current coordinates of the tardis
     * @param setCoords The coordinates that were set for the tardis
     * @param tardis_state The state of the tardis (demat / remat)
     *
     * @author Felix Eckert
     */
    public static void updateTardfile(File path, String name, int tardis_id, String uuid, int[] intCoords, int[] coords, int[] setCoords, int dimension, int setDimension, boolean[] tardis_state) throws IOException {
        path.delete(); // Delete the old tardfile

        String[] tardfile = createTardFileArray(name, uuid, tardis_id, intCoords[0], intCoords[1], intCoords[2],coords[0],coords[1], coords[2], setCoords[0], setCoords[1], setCoords[2], dimension, setDimension, tardis_state, false);

        FileUtil.writeFileFromArray(path, tardfile, FileUtil.LineMods.LN_BREAK);
        replaceChar(path);

        updateTardfileRegistry(tardis_id, name, coords, false);
    }

    /**
     * Used for updating the Tardfile Index/Registry
     * @param id
     * @param owner
     * @param xyz
     * @author FelixEcker
     * */
    public static void updateTardfileRegistry(int id, String owner, int[] xyz, boolean deleteEntry) {
        JsonObject tardfileIndex;
        File tardfileIndexFile = new File(DimensionManager.getCurrentSaveRootDirectory() + "/tardises/tardfileIndex.json"); // File Object for the Tardfile Index/Registry for writing
        try { // Parse the Tardfile Index/registry
            tardfileIndex = getRegistry();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        JsonObject updateObject = tardfileIndex.get(String.valueOf(id)).getAsJsonObject(); // Get the actual entry

        // Remove old fields/values
        updateObject.remove("xyz");
        updateObject.remove("owner");

        // Add the new Values
        updateObject.addProperty("owner", owner);
        updateObject.add("xyz", new Gson().fromJson("[{'val':'"+xyz[0]+"'}, {'val':'"+xyz[1]+"'}, {'val':'"+xyz[2]+"'}]", JsonArray.class));

        // Updated the Entry in the Index
        tardfileIndex.remove(String.valueOf(id));
        tardfileIndex.add(String.valueOf(id), updateObject);

        if (deleteEntry) {
            tardfileIndex.remove(String.valueOf(id));
        }

        // Delete old index
        if (tardfileIndexFile.exists() && tardfileIndexFile.canWrite()) {
            try { // Write new entry
                tardfileIndexFile.delete();
                FileUtil.writeFile(tardfileIndexFile, tardfileIndex.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (FileAlreadyExistsException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Replaces all single-quotes in a JSON file with double-quotes
     *
     * @param path path to the JSON file
     * @throws IOException
     * 
     * @author Felix Eckert
     */
    public static boolean replaceChar(File path) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(path.getPath()))); // Read the single-line json file to a String
        String newJSON = json.replaceAll("\\'", Character.toString('"')); // Replace all instances of the single quote with a double quote

        // Delete the old file
        if (!path.delete()) {
            Reference.logger.warning("Couldn't delete JSON file! Returning...");
            return false;
        }

        FileUtil.writeFile(path, newJSON);

        return true;
    }

    /**
     * Mainly used by the delete-tardis command
     *
     * @param path path to the file to delete
     * @param user user that called the command
     *
     * @author Felix Eckert
     */
    public static void deleteTardFile(File path, @Nullable ICommandSender user, @Nullable World world) {
        try {
            JsonObject data = FileUtil.parseJSON(path); // Parse the tardfile

            int[] coords = getCoordsFromTardfile(data); // Get the coords from the tardfile for generating a new BlockPos

            int x = coords[0]; // Get the x coordinate
            int y = coords[1]; // Get the y coordinate
            int z = coords[2]; // Get the z coordinate

            if (world != null) {
            	BlockPos tardisBlockPos = new BlockPos(x, y, z); // Create a new BlockPos
            	world.destroyBlock(tardisBlockPos, true); // Destroy the tardis
            }

            System.out.println(getTardisIDFromTardfile(data));
            System.out.println(user.getName());
            updateTardfileRegistry(getTardisIDFromTardfile(data), user.getName(), new int[]{0,0,0}, true);
            path.delete(); // Delete the tardfile
            if (user != null) {
            	user.sendMessage(new TextComponentString("§2Succesfully deleted your old TARDIS!"));
            }
        } catch (Exception e) {
            Reference.logger.warning("An Error occured whilst deleting tardis of player " + user.getName() + "!");
            if (user != null) {
            	user.sendMessage(new TextComponentString("§4An error occured whilst deleting your TARDIS!"));
            }
            e.printStackTrace();
            return;
        }
    }

    /**
     * Searches for a Json file by name and returns an JsonObject
     *
     * @param name Username
     *
     * @author Felix Eckert
     */
    public static JsonObject parseTardfileByName(String name) {
        JsonObject data;
        try {
			data = FileUtil.parseJSON(new File(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/tardises/tardFile_" + name + ".json"));
		} catch (IOException e) {
			return null;
		}

        return data;
    }

    /**
     * Searches for a Tardfile in the tardfileIndex/registry by a given TARDIS ID,
     * if found it returns the Tardfile as an JsonObject
     * 
     * @param id The ID of the TARDIS to search for
     * @throws IOException 
     * @author Felix Eckert
     * */
    public static JsonObject parseTardfileByID(int id) throws IOException, IllegalArgumentException {
    	JsonObject tardfileIndex;
    	JsonObject tardfileIndexObject;
    	JsonObject tardfile;

    	try {
            tardfileIndex = getRegistry();// Parse the tardfile regsitry/index
        } catch(IOException e) {
    	    e.printStackTrace();
    	    return null;
        }

    	if (tardfileIndex.get("registeredTardises").getAsInt() < id) { // Check if the ID is valid
    		throw new IllegalArgumentException("The TARDIS ID cannot be bigger than the number of registered TARDISes!");
    	}
    	tardfileIndexObject = tardfileIndex.get(String.valueOf(id)).getAsJsonObject(); // Get the matching tardfileIndex entry
    	tardfile = parseTardfileByName(tardfileIndexObject.get("owner").getAsString()); // Parse the matching tardfile
    	
    	return tardfile; // Finally return the given TARDFILE
    }
    
    /**
     * Searches for a Json file by name and returns a File (Object)
     *
     * @param name Username
     * @author Felix Eckert
     */
    public static File findTardfileByName(String name) {
        File data;
        data = new File(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/tardises/tardFile_" + name + ".json");

        return data;
    }

    /**
     * Searches for a Json file by XYZ and returns an int (ID of TARDIS)
     *
     * @param user Username
     * @param xyz Coordinates
     * @author Felix Eckert
     */
    public static int getTardisIDByXYZ(int[] xyz, String user) throws IOException {
        JsonObject tardfileIndex = getRegistry(); // Load the TARDFILE registry

    	// Loop through registered TARDISES
    	for (int i = 0; i < tardfileIndex.get("registeredTardises").getAsInt(); i++) {
    		JsonElement tempObj = tardfileIndex.get(String.valueOf(i+1)); // Temporary JSON-Object for current TARDIS
    		JsonArray xyzElement = tempObj.getAsJsonObject().get("xyz").getAsJsonArray(); // Get the Coordinates as a JSON Array
    		String owner = tempObj.getAsJsonObject().get("owner").getAsString(); // Get the Owner as String
    		int[] xyz2 = { // Write the Coordinates from the TARDFILE to an array
    				xyzElement.get(0).getAsJsonObject().get("val").getAsInt(),
    				xyzElement.get(1).getAsJsonObject().get("val").getAsInt(),
    				xyzElement.get(2).getAsJsonObject().get("val").getAsInt()
    		};
    		
    		if (xyz[0] == xyz2[0] && xyz[1] == xyz2[1] && xyz[2] == xyz2[2]) { // Check if the coordinates match
    		    return i+1;
    		}
    	}
    	return -2;
    }

    public static JsonObject getRegistry() throws IOException {
        File pathToRegistry = new File(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/tardises/tardfileIndex.json");
        if (!pathToRegistry.exists()) {
            throw new IOException("The Tardfile Index doesn't exist! It was either deleted or the creation of it failed.\nSee https://line6studios.github.io/projects/akhaten/problems.html#fne-tardfileindex for help!");
        }
        return FileUtil.parseJSON(pathToRegistry);
    }

    /**
     * Retrieve someones Tardis History
     *
     * @param name
     */
    public static JsonObject retrieveTardisHistory(String name) throws IOException {
        File history = new File(DimensionManager.getCurrentSaveRootDirectory()+"/tardises/tardisHistory.json");
        JsonObject historyJson = FileUtil.parseJSON(history);
        if(!historyJson.has(name)) {
            return null;
        }

        return historyJson.get(name).getAsJsonObject();
    }

    /**
     * Get the Tardis History File
     * */
    public static JsonObject getTardisHistory() {
        try {
            return FileUtil.parseJSON(new File(DimensionManager.getCurrentSaveRootDirectory()+"/tardises/tardisHistory.json"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update someones Tardis History
     * @param user
     * @param id
     * */
    public static void updateTardisHistory(String user, int id) throws IOException, NullPointerException {
        File history = new File(DimensionManager.getCurrentSaveRootDirectory()+"/tardises/tardisHistory.json"); // Path to the History
        JsonObject historyJson;
        JsonObject playerHistory = null;
        JsonArray past;

        com.sun.org.apache.xml.internal.security.Init.init();

        if (!history.exists()) {
            FileUtil.writeFile(history,"{\n}");
        }

        historyJson = FileUtil.parseJSON(history);
        if (!historyJson.has(user)) {
            historyJson.add(user, new Gson().fromJson("{'latest':0,\n  'past':[]\n}", JsonObject.class));
        }

        playerHistory = historyJson.get(user).getAsJsonObject();
        past = playerHistory.get("past").getAsJsonArray();
        playerHistory.remove("latest");
        playerHistory.add("latest", new Gson().fromJson(String.valueOf(id), JsonElement.class));

        past.add(id);
        playerHistory.remove("past");
        playerHistory.add("past", new Gson().fromJson(past.toString(), JsonArray.class));

        if (playerHistory == null) {
            throw new NullPointerException("An errror occured whilst updating the Tardis History of player "+user);
        }

        historyJson.remove(user);
        historyJson.add(user, new Gson().fromJson(playerHistory.toString(), JsonObject.class));
        history.delete();
        FileUtil.writeFile(history, historyJson.toString());
    }

    /**
     * Create the "Tardfile array" that is going to be written to the JSON file...
     *
     * @param user Name of the user that placed the tardis
     * @param uuid uuid of the user that placed the tardis
     * @param tardis_id ID of the tardis
     * @param intX the interior X-Coordinate of the tardis
     * @param intY the interior Y-Coordinate of the tardis
     * @param intZ the interior Z-Coordinate of the tardis
     * @param x the current x coordinate of the tardis
     * @param y the current y coordinate of the tardis
     * @param z the current z coordinate of the tardis
     * @param setX the x coordinate the tardis is set for
     * @param setY the y coordinate the tardis is set for
     * @param setZ the Z coordinate the tardis is set for
     * @param dimension the current dimension of the tardis
     * @param setDimension the dimension the tardis is set for
     * 
     * @author Felix Eckert
     */
    private static String[] createTardFileArray(String user, String uuid, int tardis_id, int intX, int intY, int intZ,int x, int y, int z, int setX, int setY, int setZ, int dimension, int setDimension, boolean[] state, boolean firstTimeLoadingTD) {

        String[] template;
        template = new String[]{

                "{\n  'user':'" + user + "',",
                "  'uuid':'" + uuid +  "',",
                "  'tardis_id':'"  + tardis_id +  "',",
                "  'intX':'" + intX + "',",
                "  'intY':'" + intY + "',",
                "  'intZ':'" + intZ + "',",
                "  'is_demat':'"+ state[0] +"',",
                "  'is_remat':'"+ state[1] +"',",
                "  'x':'"  + x +  "',",
                "  'y':'"  + y +  "',",
                "  'z':'" + z +  "',",
                "  'dimension':'" + dimension + "',",
                "  'setX':'"  + setX +  "',",
                "  'setY':'"  + setY +  "',",
                "  'setZ':'" + setZ +  "',",
                "  'setDimension':'" + setDimension + "',",
                "  'firstTimeLoadingTD':'" + firstTimeLoadingTD+ "',",
                "  'trustedPlayers':[],",
                "  'installedModules':{}" + "\n}"

        };

        return template;
    }

    public static boolean addCompanionToTardfile(File file, String name) {
        JsonObject data = null;
        try {
            data = FileUtil.parseJSON(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JsonArray trusted = data.get("trustedPlayers").getAsJsonArray();
        trusted.add(name);
        data.remove("trustedPlayers");
        data.add("trustedPlayers", trusted);
        file.delete();
        try {
            FileUtil.writeFile(file, data.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static boolean isCompanion(File file, String name) {
        JsonObject data = null;
        try {
            data = FileUtil.parseJSON(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JsonArray trusted = data.get("trustedPlayers").getAsJsonArray();
        for (int i = 0 ; i <= trusted.size()-1; i++) {
            if (trusted.get(i).getAsString().matches(name)) {
                return true;
            }
        }

        return false;
    }

    // Tardfile field getters below
    public static int[] getIntCoordsFromTardfile(JsonObject data) {
        int[] intCoords = {data.get("intX").getAsInt(), data.get("intY").getAsInt(), data.get("intZ").getAsInt()};
        return intCoords;
    }

    public static int[] getCoordsFromTardfile(JsonObject data) {
        int[] coords = {data.get("x").getAsInt(), data.get("y").getAsInt(), data.get("z").getAsInt()};
        return coords;
    }

    public static boolean[] getTardisStateFromTardFile(JsonObject data) {
        return new boolean[]{data.get("is_demat").getAsBoolean(), data.get("is_remat").getAsBoolean()};
    }

    public static int[] getSetCoordsFromTardfile(JsonObject data) {
        int[] setCoords = {data.get("setX").getAsInt(), data.get("setY").getAsInt(), data.get("setZ").getAsInt()};
        return setCoords;
    }

    public static int getTardisIDFromTardfile(JsonObject data) { return data.get("tardis_id").getAsInt(); }
    public static String getUUIDFromTardfile(JsonObject data) { return data.get("uuid").getAsString(); }
    public static int getDimensionFromTardfile(JsonObject data) { return data.get("dimension").getAsInt(); }
    public static int getSetDimensionFromTardfile(JsonObject data) { return data.get("setDimension").getAsInt(); }
    public static boolean getFirstTimeLoadingTD(JsonObject data) { return data.get("firstTimeLoadingTD").getAsBoolean(); }

}
