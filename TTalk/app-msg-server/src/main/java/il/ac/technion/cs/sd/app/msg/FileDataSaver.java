package il.ac.technion.cs.sd.app.msg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;

/**
 * An interface for saving and loading persistent data to a file. The concrete
 * data instance sent to this class must implement the Serializable interface.
 * For example, you can instantiate this class using List as its data type, and
 * send ArrayList to the encode method.
 * 
 * @param <T>
 *            The data type to save.
 */
public class FileDataSaver<T> implements DataSaver<T> {

	final String fileName;
	
	/**
	 * Creates a new DataSaver.
	 * @param fileName the filename to use to save and load the data.
	 */
	FileDataSaver(String fileName) {
		this.fileName = fileName;
	}
	
	/**
	 * Save data to the file.
	 * @param data the data to save.
	 */
	@Override
	public void save(T data) {
		Serializable serializableData = (Serializable) data;
		ObjectOutputStream oos = null;
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(serializableData);
			oos.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Loads the previously saved data from the file, if exists.
	 * @return the previously saved data; or an empty optional in case there is no data saved/data was cleaned.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Optional<T> load() {
		File file = new File(fileName);
		if (!file.exists()) {
			return Optional.empty();
		}
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(fis);
			T obj = (T) ois.readObject();
			return Optional.of(obj);
		} catch (IOException | ClassNotFoundException e ) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (ois != null) {
					ois.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Clears all saved data, deletes the file used to save the data.
	 */
	@Override
	public void clean() {
		File file = new File(fileName);
		if (!file.exists()) {
			return;
		}
		file.delete();
		
	}
}
