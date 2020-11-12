def get_two_channels(input='', samplerate=16000):
    playing_cut_time = 3  # in seconds
    filename = input[25:len(input)-4]
    from scipy.io import wavfile
    fs, data = wavfile.read(input)
    X_mixed = data

    from scipy.io.wavfile import write
    ch0 = np.int16(X_mixed[:, 0]/np.max(np.abs(X_mixed[:, 0])) * 32767)
    write('storage/emulated/0/AIRES/mixed_' + filename + '_channel_0.wav', samplerate, ch0)

    ch1 = np.int16(X_mixed[:, 1]/np.max(np.abs(X_mixed[:, 1])) * 32767)
    write('storage/emulated/0/AIRES/mixed_' + filename + '_channel_1.wav', samplerate, ch1)

def main(samplerate = 16000, opt_it = 30, betha = 0.8, input = '',
         maxdelay = 20, max_atten = 2, coeffs_str = '', coeffweights_orig_str = '', play_volume = 1.0):

    coeffs = np.fromstring(coeffs_str, dtype=float, sep=',')
    coeffweights_orig = np.fromstring(coeffweights_orig_str, dtype=float, sep=',')
    do_downsampling = False
    N_downsample = 2
    show_progress = False
    playing_cut_time = 3  # in seconds
    filename = input[25:len(input)-4]

    from scipy.io import wavfile
    fs, data = wavfile.read(input)
    X_mixed = data

    result_str = "***********************************" + "\nsamplerate = " + str(samplerate) \
                 + "\nmaxdelay = " + str(maxdelay) + "\nmax_atten = " + str(max_atten) + "\nplay_volume = " + str(play_volume) + \
                 "\n***********************************\n\n"
    with open('storage/emulated/0/AIRES/logs.txt', 'w') as the_file:
        the_file.write(result_str)
    print_logs(result_str)
    print_logs("beginning of main")
    import datetime
    start_time = datetime.datetime.now()
    '''###########################################################'''
    """ Open simulated convolutive mixture """

    # Normalize signal  (abs(Max_value) has to be 1)
    X_mixed = X_mixed/np.max(np.abs(X_mixed))

    cut_time = playing_cut_time * samplerate
    if cut_time > len(X_mixed[:, 0]):
        cut_time = len(X_mixed[:, 0])

    print_logs("cut time:", cut_time)


    '''###########################################################'''
    # Write end time
    print_logs("end of main")
    end_time = datetime.datetime.now()
    spent_time = end_time - start_time
    f = open('storage/emulated/0/AIRES/time.txt', 'w')
    f.write(str(spent_time))
    f.close()