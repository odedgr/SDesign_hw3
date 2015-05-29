package il.ac.technion.cs.sd.app.msg;

import java.util.Optional;

/**
 * An interface for saving and loading persistent data.
 * @param <T> The data type to save.
 */
public interface DataSaver<T> {
	/**
	 * Save the data persistently.
	 * @param data the data to save.
	 */
	void save(T data);
	
	/**
	 * Loads the previously saved data, if exists.
	 * @return the previously saved data; or an empty optional in case there is no data saved/data was cleaned.
	 */
	Optional<T> load();
	
	/**
	 * Cleans any previously saved data.
	 */
	void clean();
}
