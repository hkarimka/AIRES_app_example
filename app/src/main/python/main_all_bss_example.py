def main():
    from scipy.io import wavfile
    samplerate, X_mixed = wavfile.read('mix_3.wav')
    # print("Original signal shape", X_mixed.shape)

    # Normalize signal  (abs(Max_value) has to be 1)
    X_mixed = X_mixed/np.max(np.abs(X_mixed))

    # ToDo: Implement signal extension to 10 seconds (maximum in our evaluation)
    # Make our signal to have length 10 seconds
    # print("New signal shape", X_mixed.shape)

    # ToDo