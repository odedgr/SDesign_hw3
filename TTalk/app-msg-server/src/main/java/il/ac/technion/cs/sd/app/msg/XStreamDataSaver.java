package il.ac.technion.cs.sd.app.msg;

import java.util.Optional;

import il.ac.technion.cs.sd.msg.Codec;
import il.ac.technion.cs.sd.msg.XStreamCodec;

/**
 * An interface for saving and loading persistent data to a file. The concrete
 * data instance sent to this class must implement the Serializable interface.
 * For example, you can instantiate this class using List as its data type, and
 * send ArrayList to the encode method.
 * 
 * @param <T>
 *            The data type to save.
 */
public class XStreamDataSaver<T> implements DataSaver<T> {
	final DataSaver<String> innerSaver;
	final Codec<T> codec;
	
	/**
	 * Creates a new DataSaver.
	 * @param fileName the filename to use to save and load the data.
	 */
	XStreamDataSaver(String fileName) {
		innerSaver = new FileDataSaver<String>(fileName);
		codec = new XStreamCodec<T>();
	}
	
	/**
	 * Save data to the file.
	 * @param data the data to save.
	 */
	@Override
	public void save(T data) {
		innerSaver.save(codec.encode(data));
	}
	
	/**
	 * Loads the previously saved data from the file, if exists.
	 * @return the previously saved data; or an empty optional in case there is no data saved/data was cleaned.
	 */
	@Override
	public Optional<T> load() {
		Optional<String> loaded = innerSaver.load();
		if (!loaded.isPresent()) {
			return Optional.empty();
		}
		return Optional.of(codec.decode(loaded.get())); 
	}

	/**
	 * Clears all saved data, deletes the file used to save the data.
	 */
	@Override
	public void clean() {
		innerSaver.clean();
	}
}