package com.jact.jactfirstdemo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class JactFileUtils {
	
	public static void DeleteDir(File dir) {
		if (dir.isFile()) {
			dir.delete();
			return;
		}
		for (File f : dir.listFiles()) {
			DeleteDir(f);
		}
		dir.delete();
	}
	
	public static void CleanDir(File dir) {
		if (dir == null) return;
		DeleteDir(dir);
	}
	
	public static long GetDirSize(File dir) {
		if (dir.isFile()) {
			return dir.length();
		}
		long length = 0;
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				length += file.length();
			} else {
				length += GetDirSize(file);
			}
		}
		return length;
	}
	
	public static List<File> GetAllFilesInDir(File dir) {
		List<File> to_return = new ArrayList<File>();
		if (dir.isFile()) {
			to_return.add(dir);
			return to_return;
		}
		for (File f : dir.listFiles()) {
			to_return.addAll(GetSortedFilesByDate(f));
		}
		return to_return;
	}
	
	public static List<File> GetSortedFilesByDate(File dir) {
		List<File> to_return = GetAllFilesInDir(dir);
		Collections.sort(to_return, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				long diff = f1.lastModified() - f2.lastModified();
				if (diff == 0) return 0;
				if (diff < 0) return -1;
				return 1;
			}
		});
		return to_return;
	}
	
	public static void TrimDirToMaxSize(File dir, long max_size) {
		long current_size = GetDirSize(dir);
		if (current_size <= max_size) return;
		if (dir.isFile()) {
			dir.delete();
			return;
		}
		List<File> files_by_date = GetSortedFilesByDate(dir);
		long space_needed = current_size - max_size;
		long space_freed = 0;
		for (File f : files_by_date) {
			if (f.isDirectory()) {
				continue;
			}
			space_freed += f.length();
			f.delete();
			if (space_freed >= space_needed) {
				break;
			}
		}
	}
}